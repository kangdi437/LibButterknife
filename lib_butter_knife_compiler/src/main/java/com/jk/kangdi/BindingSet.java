package com.jk.kangdi;

import com.google.common.collect.ImmutableList;
import com.jk.kangdi.internal.ListenerClass;
import com.jk.kangdi.internal.ListenerMethod;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreElements.getPackage;
import static com.jk.kangdi.LibButterknifeProcessor.ACTIVITY_TYPE;
import static com.jk.kangdi.LibButterknifeProcessor.DIALOG_TYPE;
import static com.jk.kangdi.LibButterknifeProcessor.VIEW_TYPE;
import static com.jk.kangdi.LibButterknifeProcessor.isSubtypeOfType;
import static com.squareup.javapoet.ClassName.bestGuess;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by JINKANG on 2017/7/24.
 */

public class BindingSet {

    static final ClassName UTILS = ClassName.get("com.jk.kangdi.lib_butter_knife_api", "Utils");
    private static final ClassName UNBINDER = ClassName.get("com.jk.kangdi.lib_butter_knife_api", "Unbinder");
    private static final ClassName UI_THREAD =
            ClassName.get("android.support.annotation", "UiThread");
    private static final ClassName CALL_SUPER =
            ClassName.get("android.support.annotation", "CallSuper");
    private static final ClassName CONTEXT = ClassName.get("android.content", "Context");
    private static final ClassName VIEW = ClassName.get("android.view", "View");
    private static final ClassName SUPPRESS_LINT =
            ClassName.get("android.annotation", "SuppressLint");
    private static final ClassName RESOURCES = ClassName.get("android.content.res", "Resources");

    private final TypeName targetTypeName;
    private final ClassName bindingClassName;
    private final boolean isFinal;
    private final boolean isView;
    private final boolean isActivity;
    private final boolean isDialog;
    private final ImmutableList<ViewBinding> viewBindings;
    private final ImmutableList<FieldCollectionViewBinding> collectionBindings;
    private final ImmutableList<ResourceBinding> resourceBindings;
    private final ContentViewBinding contentBinding;
    private final BindingSet parentBinding;

    private BindingSet(TypeName targetTypeName, ClassName bindingClassName, boolean isFinal,
                       boolean isView, boolean isActivity, boolean isDialog, ImmutableList<ViewBinding> viewBindings,
                       ImmutableList<FieldCollectionViewBinding> collectionBindings,
                       ImmutableList<ResourceBinding> resourceBindings, BindingSet parentBinding ,
                       ContentViewBinding contentBinding) {
        this.isFinal = isFinal;
        this.targetTypeName = targetTypeName;
        this.bindingClassName = bindingClassName;
        this.isView = isView;
        this.isActivity = isActivity;
        this.isDialog = isDialog;
        this.viewBindings = viewBindings;
        this.collectionBindings = collectionBindings;
        this.resourceBindings = resourceBindings;
        this.parentBinding = parentBinding;
        this.contentBinding = contentBinding;
    }

    public static Builder newBuilder(TypeElement enclosingElement){
        TypeMirror typeMirror = enclosingElement.asType();

        boolean isView = isSubtypeOfType(typeMirror, VIEW_TYPE);
        boolean isActivity = isSubtypeOfType(typeMirror, ACTIVITY_TYPE);
        boolean isDialog = isSubtypeOfType(typeMirror, DIALOG_TYPE);

        TypeName targetType = TypeName.get(typeMirror);
        if (targetType instanceof ParameterizedTypeName) {
            targetType = ((ParameterizedTypeName) targetType).rawType;
        }

        String packageName = getPackage(enclosingElement).getQualifiedName().toString();
        String className = enclosingElement.getQualifiedName().toString().substring(
                packageName.length() + 1).replace('.', '$');
        ClassName bindingClassName = ClassName.get(packageName, className + "_ViewBinding");

        boolean isFinal = enclosingElement.getModifiers().contains(FINAL);
        return new Builder(targetType, bindingClassName, isFinal, isView, isActivity, isDialog);
    }

    JavaFile brewJava(int sdk, boolean debuggable) {
        return JavaFile.builder(bindingClassName.packageName(), createType(sdk))
                .addFileComment("Generated code. Do not modify!")
                .build();
    }
    private TypeSpec createType(int sdk) {
        //创建类 public class XXX
        TypeSpec.Builder result = TypeSpec.classBuilder(bindingClassName.simpleName())
                .addModifiers(PUBLIC);
        if (isFinal) {
            result.addModifiers(FINAL);
        }

        //parenBinding不为空则继承父类 , 为空则实现UNBINDER
        //暂时不做父类的处理
//        if (parentBinding != null) {
//            result.superclass(parentBinding.bindingClassName);
//        } else {
        result.addSuperinterface(UNBINDER);
//        }

        //添加成员变量
        if (hasTargetField()) {
            //private 类 target
            //一般为Activity , Dialog , View
            result.addField(targetTypeName, "target", PRIVATE);
        }

        //根据上面添加的成员变量类型判断 , 创建一个参数的构造方法
        //单个构造方法貌似并没有什么卵用
//        if (isView) {
//            result.addMethod(createBindingConstructorForView());
//        } else if (isActivity) {
//            result.addMethod(createBindingConstructorForActivity());
//        } else if (isDialog) {
//            result.addMethod(createBindingConstructorForDialog());
//        }

//        if (!constructorNeedsView()) {
//            // Add a delegating constructor with a target type + view signature for reflective use.
//            result.addMethod(createBindingViewDelegateConstructor());
//        }

        //在创建一个构造方法 , 这个构造方法中实现所有的初始化
        result.addMethod(createBindingConstructor(sdk));

        if (hasViewBindings() || parentBinding == null) {
            result.addMethod(createBindingUnbindMethod(result));
        }

        return result.build();
    }

    private MethodSpec createBindingUnbindMethod(TypeSpec.Builder bindingClass) {
        MethodSpec.Builder result = MethodSpec.methodBuilder("unbind")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC);
        if (!isFinal && parentBinding == null) {
            result.addAnnotation(CALL_SUPER);
        }

        if (hasTargetField()) {
            if (hasFieldBindings()) {
                result.addStatement("$T target = this.target", targetTypeName);
            }
            result.addStatement("if (target == null) throw new $T($S)", IllegalStateException.class,
                    "Bindings already cleared.");
            result.addStatement("$N = null", hasFieldBindings() ? "this.target" : "target");
            result.addCode("\n");
            for (ViewBinding binding : viewBindings) {
                if (binding.getFieldBinding() != null) {
                    result.addStatement("target.$L = null", binding.getFieldBinding().getName());
                }
            }
            for (FieldCollectionViewBinding binding : collectionBindings) {
                result.addStatement("target.$L = null", binding.name);
            }
        }

        if (hasMethodBindings()) {
            result.addCode("\n");
            for (ViewBinding binding : viewBindings) {
                addFieldAndUnbindStatement(bindingClass, result, binding);
            }
        }

        if (parentBinding != null) {
            result.addCode("\n");
            result.addStatement("super.unbind()");
        }
        return result.build();
    }


    private void addFieldAndUnbindStatement(TypeSpec.Builder result, MethodSpec.Builder unbindMethod,
                                            ViewBinding bindings) {
        // Only add fields to the binding if there are method bindings.
        Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> classMethodBindings =
                bindings.getMethodBindings();
        if (classMethodBindings.isEmpty()) {
            return;
        }

        String fieldName = bindings.isBoundToRoot() ? "viewSource" : "view" + bindings.getId().value;
        result.addField(VIEW, fieldName, PRIVATE);

        // We only need to emit the null check if there are zero required bindings.
        boolean needsNullChecked = bindings.getRequiredBindings().isEmpty();
        if (needsNullChecked) {
            unbindMethod.beginControlFlow("if ($N != null)", fieldName);
        }

        for (ListenerClass listenerClass : classMethodBindings.keySet()) {
            // We need to keep a reference to the listener
            // in case we need to unbind it via a remove method.
            boolean requiresRemoval = !"".equals(listenerClass.remover());
            String listenerField = "null";
            if (requiresRemoval) {
                TypeName listenerClassName = bestGuess(listenerClass.type());
                listenerField = fieldName + ((ClassName) listenerClassName).simpleName();
                result.addField(listenerClassName, listenerField, PRIVATE);
            }

            if (!VIEW_TYPE.equals(listenerClass.targetType())) {
                unbindMethod.addStatement("(($T) $N).$N($N)", bestGuess(listenerClass.targetType()),
                        fieldName, removerOrSetter(listenerClass, requiresRemoval), listenerField);
            } else {
                unbindMethod.addStatement("$N.$N($N)", fieldName,
                        removerOrSetter(listenerClass, requiresRemoval), listenerField);
            }

            if (requiresRemoval) {
                unbindMethod.addStatement("$N = null", listenerField);
            }
        }

        unbindMethod.addStatement("$N = null", fieldName);

        if (needsNullChecked) {
            unbindMethod.endControlFlow();
        }
    }


    private String removerOrSetter(ListenerClass listenerClass, boolean requiresRemoval) {
        return requiresRemoval
                ? listenerClass.remover()
                : listenerClass.setter();
    }


    /** True when this type's bindings use raw integer values instead of {@code R} references. */
    private boolean hasUnqualifiedResourceBindings() {
        for (ResourceBinding binding : resourceBindings) {
            if (!binding.id().qualifed) {
                return true;
            }
        }
        return false;
    }


    private MethodSpec createBindingConstructor(int sdk) {
        //添加注解
        // @UiThread
        // public
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(PUBLIC);

        if (hasMethodBindings()) {
            constructor.addParameter(targetTypeName, "target", FINAL);
        } else {
            constructor.addParameter(targetTypeName, "target");
        }

        //需要View还是 , 需要Context
        if (constructorNeedsView()) {
            constructor.addParameter(VIEW, "source");
        } else {
            constructor.addParameter(CONTEXT, "context");
        }

        if (hasUnqualifiedResourceBindings()) {
            // Aapt can change IDs out from underneath us, just suppress since all will work at runtime.
            constructor.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ResourceType")
                    .build());
        }

//        if (hasOnTouchMethodBindings()) {
//            constructor.addAnnotation(AnnotationSpec.builder(SUPPRESS_LINT)
//                    .addMember("value", "$S", "ClickableViewAccessibility")
//                    .build());
//        }

        //不考虑父类
//        if (parentBinding != null) {
//            if (parentBinding.constructorNeedsView()) {
//                constructor.addStatement("super(target, source)");
//            } else if (constructorNeedsView()) {
//                constructor.addStatement("super(target, source.getContext())");
//            } else {
//                constructor.addStatement("super(target, context)");
//            }
//            constructor.addCode("\n");
//        }

        if (hasTargetField()) {
            constructor.addStatement("this.target = target");
            constructor.addCode("\n");
        }

        if (hasContentBinding()){
            constructor.addStatement("$L" , contentBinding.render());
        }

        if (hasViewBindings()) {
//            if (hasViewLocal()) {
//                // Local variable in which all views will be temporarily stored.
//                constructor.addStatement("$T view", VIEW);
//            }

            //实例化BindView
            for (ViewBinding binding : viewBindings) {
                addViewBinding(constructor, binding);
            }
            //实例化BindViews
            for (FieldCollectionViewBinding binding : collectionBindings) {
                constructor.addStatement("$L", binding.render());
            }

            if (!resourceBindings.isEmpty()) {
                constructor.addCode("\n");
            }
        }

        if (!resourceBindings.isEmpty()) {
            if (constructorNeedsView()) {
                constructor.addStatement("$T context = source.getContext()", CONTEXT);
            }
            if (hasResourceBindingsNeedingResource(sdk)) {
                constructor.addStatement("$T res = context.getResources()", RESOURCES);
            }
            for (ResourceBinding binding : resourceBindings) {
                constructor.addStatement("$L", binding.render(sdk));
            }
        }

        return constructor.build();
    }

    private void  addViewBinding(MethodSpec.Builder result, ViewBinding binding) {
        if (binding.isSingleFieldBinding()) {
            // Optimize the common case where there's a single binding directly to a field.
            FieldViewBinding fieldBinding = binding.getFieldBinding();
            //ex: target.btn =
            CodeBlock.Builder builder = CodeBlock.builder()
                    .add("target.$L = ", fieldBinding.getName());

            //判断属性是否是 View , 如果是View 直接调用findViewById()
            boolean requiresCast = requiresCast(fieldBinding.getType());
            builder.add("$T.find", UTILS);
            builder.add(fieldBinding.isRequired() ? "RequiredView" : "OptionalView");
            if (requiresCast) {
                builder.add("AsType");
            }
            builder.add("(source, $L", binding.getId().code);
            //描述
            if (fieldBinding.isRequired() || requiresCast) {
                builder.add(", $S", asHumanDescription(singletonList(fieldBinding)));
            }
            if (requiresCast) {
                builder.add(", $T.class", fieldBinding.getRawType());
            }
            builder.add(")");
            result.addStatement("$L", builder.build());
            return;
        }

        //这里做方法处理
//        List<MemberViewBinding> requiredBindings = binding.getRequiredBindings();
//        if (!debuggable || requiredBindings.isEmpty()) {
//            result.addStatement("view = source.findViewById($L)", binding.getId().code);
//        } else if (!binding.isBoundToRoot()) {
//            result.addStatement("view = $T.findRequiredView(source, $L, $S)", UTILS,
//                    binding.getId().code, asHumanDescription(requiredBindings));
//        }
//
//        addFieldBinding(result, binding, debuggable);
//        addMethodBindings(result, binding, debuggable);
    }

    static String asHumanDescription(Collection<? extends MemberViewBinding> bindings) {
        Iterator<? extends MemberViewBinding> iterator = bindings.iterator();
        switch (bindings.size()) {
            case 1:
                return iterator.next().getDescription();
            case 2:
                return iterator.next().getDescription() + " and " + iterator.next().getDescription();
            default:
                StringBuilder builder = new StringBuilder();
                for (int i = 0, count = bindings.size(); i < count; i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    if (i == count - 1) {
                        builder.append("and ");
                    }
                    builder.append(iterator.next().getDescription());
                }
                return builder.toString();
        }
    }

    static boolean requiresCast(TypeName type) {
        return !VIEW_TYPE.equals(type.toString());
    }

    /** True when this type's bindings use Resource directly instead of Context. */
    private boolean hasResourceBindingsNeedingResource(int sdk) {
        for (ResourceBinding binding : resourceBindings) {
            if (binding.requiresResources(sdk)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasViewLocal() {
        for (ViewBinding bindings : viewBindings) {
            if (bindings.requiresLocal()) {
                return true;
            }
        }
        return false;
    }

    private MethodSpec createBindingConstructorForDialog() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(PUBLIC)
                .addParameter(targetTypeName, "target");
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target.getWindow().getDecorView())");
        } else {
            builder.addStatement("this(target, target.getContext())");
        }
        return builder.build();
    }


    private MethodSpec createBindingConstructorForActivity() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(PUBLIC)
                .addParameter(targetTypeName, "target");
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target.getWindow().getDecorView())");
        } else {
            builder.addStatement("this(target, target)");
        }
        return builder.build();
    }
    private boolean hasTargetField() {
        return hasFieldBindings() || hasMethodBindings();
    }

    private boolean hasContentBinding(){
        return contentBinding != null;
    }

    //是否绑定的方法 , 例如onClick
    private boolean hasMethodBindings() {
        for (ViewBinding bindings : viewBindings) {
            if (!bindings.getMethodBindings().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    //是否有BindView , 或者是否有BindViwes
    private boolean hasFieldBindings() {
        for (ViewBinding bindings : viewBindings) {
            if (bindings.getFieldBinding() != null) {
                return true;
            }
        }
        return !collectionBindings.isEmpty();
    }

    private MethodSpec createBindingConstructorForView() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(PUBLIC)
                .addParameter(targetTypeName, "target");
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target)");
        } else {
            builder.addStatement("this(target, target.getContext())");
        }
        return builder.build();
    }

    /** True if this binding requires a view. Otherwise only a context is needed. */
    private boolean constructorNeedsView() {
        return hasViewBindings() //
                || parentBinding != null && parentBinding.constructorNeedsView();
    }


    /** True when this type's bindings require a view hierarchy. */
    private boolean hasViewBindings() {
        return !viewBindings.isEmpty() || !collectionBindings.isEmpty();
    }

    public static class Builder{

        private final TypeName targetTypeName;
        private final ClassName bindingClassName;
        private final boolean isFinal;
        private final boolean isView;
        private final boolean isActivity;
        private final boolean isDialog;
        private final ImmutableList.Builder<FieldCollectionViewBinding> collectionBindings =
                ImmutableList.builder();
        private final ImmutableList.Builder<ResourceBinding> resourceBindings = ImmutableList.builder();
        private BindingSet parentBinding;
        private ContentViewBinding contentBinding;

        private Builder(TypeName targetTypeName, ClassName bindingClassName, boolean isFinal,
                        boolean isView, boolean isActivity, boolean isDialog) {
            this.targetTypeName = targetTypeName;
            this.bindingClassName = bindingClassName;
            this.isFinal = isFinal;
            this.isView = isView;
            this.isActivity = isActivity;
            this.isDialog = isDialog;
        }

        void setParent(BindingSet parent) {
            this.parentBinding = parent;
        }

        private final Map<Id, ViewBinding.Builder> viewIdMap = new LinkedHashMap<>();

        String findExistingBindingName(Id id) {
            ViewBinding.Builder builder = viewIdMap.get(id);
            if (builder == null) {
                return null;
            }
            FieldViewBinding fieldBinding = builder.fieldBinding;
            if (fieldBinding == null) {
                return null;
            }
            return fieldBinding.getName();
        }

        void addField(Id id, FieldViewBinding binding) {
            getOrCreateViewBindings(id).setFieldBinding(binding);
        }

        //存储<Id , ViewBinding.Builder>
        private ViewBinding.Builder getOrCreateViewBindings(Id id) {
            ViewBinding.Builder viewId = viewIdMap.get(id);
            if (viewId == null) {
                viewId = new ViewBinding.Builder(id);
                viewIdMap.put(id, viewId);
            }
            return viewId;
        }

        void addFieldCollection(FieldCollectionViewBinding binding) {
            collectionBindings.add(binding);
        }

        public void setContentBinding(ContentViewBinding contentBinding){
            this.contentBinding = contentBinding;
        }

        BindingSet build() {
            //存储 <ViewBinding>
            ImmutableList.Builder<ViewBinding> viewBindings = ImmutableList.builder();
            for (ViewBinding.Builder builder : viewIdMap.values()) {
                viewBindings.add(builder.build());
            }
            return new BindingSet(targetTypeName, bindingClassName, isFinal, isView, isActivity, isDialog,
                    viewBindings.build(), collectionBindings.build(), resourceBindings.build(),
                    parentBinding , contentBinding);
        }

    }

}
