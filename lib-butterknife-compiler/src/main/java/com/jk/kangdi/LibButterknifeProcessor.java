package com.jk.kangdi;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.jk.kangdi.internal.Convert;
import com.jk.kangdi.internal.ListenerClass;
import com.jk.kangdi.internal.ListenerMethod;
import com.jk.kangdi.internal.QuickAdapter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static com.jk.kangdi.BindingSet.BASE_VIEWHOLDER;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Created by JINKANG on 2017/7/24.
 */

@AutoService(Processor.class)
public class LibButterknifeProcessor extends AbstractProcessor {

    static final String VIEW_TYPE = "android.view.View";
    static final String ACTIVITY_TYPE = "android.app.Activity";
    static final String DIALOG_TYPE = "android.app.Dialog";
    private static final String NULLABLE_ANNOTATION_NAME = "Nullable";
    static final Id NO_ID = new Id("-1");
    private static final String LIST_TYPE = List.class.getCanonicalName();
//    private static final List<Class<? extends Annotation>> LISTENERS = Arrays.asList(
//            OnClick.class
//    );

    private final Map<QualifiedId, Id> symbols = new LinkedHashMap<>();
    private Elements elementUtils;
    private Messager mMessager;
    private Filer filer;
    private Types typeUtils;


    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
        mMessager = env.getMessager();
        filer = env.getFiler();
        typeUtils = env.getTypeUtils();


    }

    /**
     * 此方法用来设置支持的注解类型，没有设置的无效（获取不到）
     * */
    @Override
    public Set<String> getSupportedAnnotationTypes() {

        HashSet<String> supportTypes = new LinkedHashSet<>();
        // 把支持的类型添加进去
        supportTypes.add(BindView.class.getCanonicalName());
        supportTypes.add(BindViews.class.getCanonicalName());
        supportTypes.add(ContentView.class.getCanonicalName());
        supportTypes.add(OnClick.class.getCanonicalName());
        supportTypes.add(QuickAdapter.class.getCanonicalName());
        supportTypes.add(Convert.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Map<TypeElement, BindingSet> bindingMap = findAndParseTargets(env);

        for (Map.Entry<TypeElement, BindingSet> entry : bindingMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BindingSet binding = entry.getValue();

            JavaFile javaFile = binding.brewJava(0, true);
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Unable to write binding for type %s: %s", typeElement, e.getMessage());
            }
        }

        return false;
    }
    private Map<TypeElement, BindingSet> findAndParseTargets(RoundEnvironment env) {

        //存储 <外部元素 , BindingSet.Builder>
        Map<TypeElement, BindingSet.Builder> builderMap = new LinkedHashMap<>();
        //存储 外部元素 一般指类
        Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();


        // Process each @BindView element.
        for (Element element : env.getElementsAnnotatedWith(BindView.class)) {
            // we don't SuperficialValidation.validateElement(element)
            // so that an unresolved View type can be generated by later processing rounds
            try {
                parseBindView(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, BindView.class, e);
            }
        }

        // Process each @BindViews element.
        for (Element element : env.getElementsAnnotatedWith(BindViews.class)) {
            // we don't SuperficialValidation.validateElement(element)
            // so that an unresolved View type can be generated by later processing rounds
            try {
                parseBindViews(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, BindViews.class, e);
            }
        }

        for (Element element : env.getElementsAnnotatedWith(ContentView.class)) {
            try {
                parseContentView(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, ContentView.class, e);
            }
        }

        for (Element element : env.getElementsAnnotatedWith(QuickAdapter.class)) {
            try {
                parseQuickAdapter(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, QuickAdapter.class, e);
            }
        }
        for (Element element : env.getElementsAnnotatedWith(Convert.class)) {
            try {
                parseConvert(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, Convert.class, e);
            }
        }

        // Process each annotation that corresponds to a listener.
//        for (Class<? extends Annotation> listener : LISTENERS) {
            findAndParseListener(env, OnClick.class, builderMap, erasedTargetNames);
//        }

            // Associate superclass binders with their subclass binders. This is a queue-based tree walk
        // which starts at the roots (superclasses) and walks to the leafs (subclasses).
        Deque<Map.Entry<TypeElement, BindingSet.Builder>> entries =
                new ArrayDeque<>(builderMap.entrySet());

        //存储 <外部元素 , BindingSet>
        Map<TypeElement, BindingSet> bindingMap = new LinkedHashMap<>();
        while (!entries.isEmpty()) {
            Map.Entry<TypeElement, BindingSet.Builder> entry = entries.removeFirst();

            TypeElement type = entry.getKey();
            BindingSet.Builder builder = entry.getValue();

//            TypeElement parentType = findParentType(type, erasedTargetNames);
            TypeElement parentType = null;
            if (parentType == null) {
                bindingMap.put(type, builder.build());
            } else {
                BindingSet parentBinding = bindingMap.get(parentType);
                if (parentBinding != null) {
                    builder.setParent(parentBinding);
                    bindingMap.put(type, builder.build());
                } else {
                    // Has a superclass binding but we haven't built it yet. Re-enqueue for later.
                    entries.addLast(entry);
                }
            }
        }

        return bindingMap;
    }



    /** Finds the parent binder type in the supplied set, if any. */
    private TypeElement findParentType(TypeElement typeElement, Set<TypeElement> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement)) {
                return typeElement;
            }
        }
    }


    private void logParsingError(Element element, Class<? extends Annotation> annotation,
                                 Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        error(element, "Unable to parse @%s binding.\n\n%s", annotation.getSimpleName(), stackTrace);
    }


    private static boolean isFieldRequired(Element element) {
        return !hasAnnotationWithName(element, NULLABLE_ANNOTATION_NAME);
    }

    private static boolean hasAnnotationWithName(Element element, String simpleName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private Id getId(QualifiedId qualifiedId) {
        if (symbols.get(qualifiedId) == null) {
            symbols.put(qualifiedId, new Id(qualifiedId.id));
        }
        return symbols.get(qualifiedId);
    }


    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    //判断是否是接口
    private boolean isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType
                && ((DeclaredType) typeMirror).asElement().getKind() == INTERFACE;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
    }

    private BindingSet.Builder getOrCreateBindingBuilder(
            Map<TypeElement, BindingSet.Builder> builderMap, TypeElement enclosingElement) {
        BindingSet.Builder builder = builderMap.get(enclosingElement);
        if (builder == null) {
            builder = BindingSet.newBuilder(enclosingElement);
            builderMap.put(enclosingElement, builder);
        }
        return builder;
    }

    /** Uses both {@link Types#erasure} and string manipulation to strip any generic types. */
    private String doubleErasure(TypeMirror elementType) {
        String name = typeUtils.erasure(elementType).toString();
        int typeParamStart = name.indexOf('<');
        if (typeParamStart != -1) {
            name = name.substring(0, typeParamStart);
        }
        return name;
    }


    private void parseContentView(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
                                Set<TypeElement> erasedTargetNames) {

        boolean hasError = false;
        if (!(element instanceof TypeElement)){
            hasError = true;
            return;
        }

        TypeElement typeElement = (TypeElement) element;

        String qualifiedName = typeElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    ContentView.class.getSimpleName(), qualifiedName);
            hasError = true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    ContentView.class.getSimpleName(), qualifiedName);
            hasError = true;
        }

        TypeMirror elementType = element.asType();

        if (!isSubtypeOfType(elementType , ACTIVITY_TYPE)){
            error(element , "%s annotate with @%s must be a subclass of activity" , ContentView.class
            .getSimpleName() , elementType);
        }

        if (hasError){
            return;
        }

        String value = element.getAnnotation(ContentView.class).value();

        BindingSet.Builder builder = getOrCreateBindingBuilder(builderMap, typeElement);

        builder.setContentBinding(new ContentViewBinding(typeElement.getSimpleName().toString() , value));

    }




    private void parseQuickAdapter(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
                                   Set<TypeElement> erasedTargetNames) {

        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        boolean hasError = isInaccessibleViaGeneratedCode(QuickAdapter.class, "fields", element)
                || isBindingInWrongPackage(QuickAdapter.class, element);


        if (hasError) {
            return;
        }

        AdapterBinding adapterBinding = new AdapterBinding();
        QuickAdapter annotation = element.getAnnotation(QuickAdapter.class);
        TypeMirror typeMirror = element.asType();
        if (typeMirror instanceof DeclaredType){
            DeclaredType d = (DeclaredType) typeMirror;
            List<? extends TypeMirror> typeArguments = d.getTypeArguments();
            for (TypeMirror t : typeArguments) {
                if (t.toString().equals(BASE_VIEWHOLDER.toString())) continue;
                adapterBinding.beanType = ClassName.get(t);
            }
        }

        adapterBinding.layoutResId = annotation.value();
        adapterBinding.filedName = element.getSimpleName().toString();

        BindingSet.Builder builder = builderMap.get(enclosingElement);

        builder.addAdapterBinding(adapterBinding);

    }
    /**
     * @param element
     * @param builderMap
     * @param erasedTargetNames
     */
    public void parseBindView(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
                              Set<TypeElement> erasedTargetNames){
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        //判断是否是静态或者私有的以及判断包含该元素的类不是android.或java.打头
        boolean hasError = isInaccessibleViaGeneratedCode(BindView.class, "fields", element)
                || isBindingInWrongPackage(BindView.class, element);

        //返回此元素定义的类型。
        TypeMirror elementType = element.asType();

        //获取类的完全限定名
        Name qualifiedName = enclosingElement.getQualifiedName();
        Name simpleName = element.getSimpleName();

        if (!isSubtypeOfType(elementType, VIEW_TYPE) && !isInterface(elementType)) {
            if (elementType.getKind() == TypeKind.ERROR) {
                note(element, "@%s field with unresolved type (%s) "
                                + "must elsewhere be generated as a View or interface. (%s.%s)",
                        BindView.class.getSimpleName(), elementType, qualifiedName, simpleName);
            } else {
                error(element, "@%s fields must extend from View or be an interface. (%s.%s)",
                        BindView.class.getSimpleName(), qualifiedName, simpleName);
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        //获取注解的value
        String value = element.getAnnotation(BindView.class).value();

        //封装id
        QualifiedId qualifiedId = new QualifiedId(elementUtils.getPackageOf(element).getQualifiedName().toString() , value);

        //builderMap <属性外层的类 , BindingSet>
        BindingSet.Builder builder = builderMap.get(enclosingElement);

        //避免重复
        if (builder != null) {
            String existingBindingName = builder.findExistingBindingName(getId(qualifiedId));
            if (existingBindingName != null) {
                error(element, "Attempt to use @%s for an already bound ID %d on '%s'. (%s.%s)",
                        BindView.class.getSimpleName(), 1, existingBindingName,
                        enclosingElement.getQualifiedName(), element.getSimpleName());
                return;
            }
        } else {
            builder = getOrCreateBindingBuilder(builderMap, enclosingElement);
        }

        //simple类名
        String name = simpleName.toString();
        //包含包名的类
        TypeName type = TypeName.get(elementType);

        //是否是必须实例化 , 检查是否有Nullable注解
        boolean required = isFieldRequired(element);

        //builder 添加属性
        builder.addField(getId(qualifiedId), new FieldViewBinding(name, type, required));

        // Add the type-erased version to the valid binding targets set.
        erasedTargetNames.add(enclosingElement);
    }

    private void findAndParseListener(RoundEnvironment env,
                                      Class<? extends Annotation> annotationClass,
                                      Map<TypeElement, BindingSet.Builder> builderMap, Set<TypeElement> erasedTargetNames) {
        for (Element element : env.getElementsAnnotatedWith(annotationClass)) {
            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                parseListenerAnnotation(annotationClass, element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));

                error(element, "Unable to generate view binder for @%s.\n\n%s",
                        annotationClass.getSimpleName(), stackTrace.toString());
            }
        }
    }


    private void parseConvert(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
                              Set<TypeElement> erasedTargetNames) {

        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        if (!(element instanceof ExecutableElement) || element.getKind() != METHOD) {
            throw new IllegalStateException(
                    String.format("@%s annotation must be on a method.", Convert.class.getSimpleName()));
        }

        ExecutableElement executableElement = (ExecutableElement) element;

        //根据以下规则去寻找匹配的QuickAdapter
        //1.根据对应的属性名称匹配
        //2.根据item的布局去匹配
        //3.根据beanType去匹配
        //一般情况下只有一个适配器 , Convert注解默认就可以 , 如果beanType在一样的情况下可以传item布局
        //两者都一样的极端情况下可以根据适配器的属性名称去匹配

        String fieldName = executableElement.getAnnotation(Convert.class).itemLayoutId();
        String itemId = executableElement.getAnnotation(Convert.class).itemLayoutId();
        String methodName = executableElement.getSimpleName().toString();

        List<? extends VariableElement> parameters = executableElement.getParameters();

        BindingSet.Builder builder = builderMap.get(enclosingElement);
        ImmutableList<AdapterBinding> adapterBindings = builder.getAdapterBinding();

        for (AdapterBinding adapterBinding : adapterBindings) {
            if (!"".equals(fieldName)){
                if (fieldName.equals(adapterBinding.filedName)){
                    adapterBinding.convertName = methodName;
                    break;
                }
            }
            if (!"".equals(itemId)){
                if (fieldName.equals(adapterBinding.layoutResId)){
                    adapterBinding.convertName = methodName;
                    break;
                }
            }
            for (VariableElement variableElement : parameters) {
                TypeMirror typeMirror = variableElement.asType();
                if (typeMirror.toString().equals(adapterBinding.beanType.toString())){
                    adapterBinding.convertName = methodName;
                }
            }
        }
    }

    private void parseListenerAnnotation(Class<? extends Annotation> annotationClass, Element element,
                                         Map<TypeElement, BindingSet.Builder> builderMap, Set<TypeElement> erasedTargetNames)
            throws Exception {
        // This should be guarded by the annotation's @Target but it's worth a check for safe casting.
        if (!(element instanceof ExecutableElement) || element.getKind() != METHOD) {
            throw new IllegalStateException(
                    String.format("@%s annotation must be on a method.", annotationClass.getSimpleName()));
        }

        ExecutableElement executableElement = (ExecutableElement) element;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Assemble information on the method.
        Annotation annotation = element.getAnnotation(annotationClass);
        Method annotationValue = annotationClass.getDeclaredMethod("value");
        if (annotationValue.getReturnType() != String[].class) {
            throw new IllegalStateException(
                    String.format("@%s annotation value() type not String[].", annotationClass));
        }


        String[] ids = (String[]) annotationValue.invoke(annotation);
        String name = executableElement.getSimpleName().toString();
        boolean hasError = isInaccessibleViaGeneratedCode(annotationClass, "methods", element);
        hasError |= isBindingInWrongPackage(annotationClass, element);

        String duplicateId = findDuplicate(ids);
        if (duplicateId != null) {
            error(element, "@%s annotation for method contains duplicate ID %d. (%s.%s)",
                    annotationClass.getSimpleName(), duplicateId, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        ListenerClass listener = annotationClass.getAnnotation(ListenerClass.class);
        if (listener == null) {
            throw new IllegalStateException(
                    String.format("No @%s defined on @%s.", ListenerClass.class.getSimpleName(),
                            annotationClass.getSimpleName()));
        }

        for (String id : ids) {
            if ("-1".equals(id)) {
                if (ids.length == 1) {
                } else {
                    error(element, "@%s annotation contains invalid ID %d. (%s.%s)",
                            annotationClass.getSimpleName(), id, enclosingElement.getQualifiedName(),
                            element.getSimpleName());
                    hasError = true;
                }
            }
        }

        ListenerMethod method;
        ListenerMethod[] methods = listener.method();
        if (methods.length > 1) {
            throw new IllegalStateException(String.format("Multiple listener methods specified on @%s.",
                    annotationClass.getSimpleName()));
        } else if (methods.length == 1) {
            if (listener.callbacks() != ListenerClass.NONE.class) {
                throw new IllegalStateException(
                        String.format("Both method() and callback() defined on @%s.",
                                annotationClass.getSimpleName()));
            }
            method = methods[0];
        } else {
            Method annotationCallback = annotationClass.getDeclaredMethod("callback");
            Enum<?> callback = (Enum<?>) annotationCallback.invoke(annotation);
            Field callbackField = callback.getDeclaringClass().getField(callback.name());
            method = callbackField.getAnnotation(ListenerMethod.class);
            if (method == null) {
                throw new IllegalStateException(
                        String.format("No @%s defined on @%s's %s.%s.", ListenerMethod.class.getSimpleName(),
                                annotationClass.getSimpleName(), callback.getDeclaringClass().getSimpleName(),
                                callback.name()));
            }
        }

        // Verify that the method has equal to or less than the number of parameters as the listener.
        List<? extends VariableElement> methodParameters = executableElement.getParameters();
        if (methodParameters.size() > method.parameters().length) {
            error(element, "@%s methods can have at most %s parameter(s). (%s.%s)",
                    annotationClass.getSimpleName(), method.parameters().length,
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }

        // Verify method return type matches the listener.
        TypeMirror returnType = executableElement.getReturnType();
        if (returnType instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) returnType;
            returnType = typeVariable.getUpperBound();
        }
        if (!returnType.toString().equals(method.returnType())) {
            error(element, "@%s methods must have a '%s' return type. (%s.%s)",
                    annotationClass.getSimpleName(), method.returnType(),
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }

        if (hasError) {
            return;
        }
        Parameter[] parameters = Parameter.NONE;
        if (!methodParameters.isEmpty()) {
            parameters = new Parameter[methodParameters.size()];
            BitSet methodParameterUsed = new BitSet(methodParameters.size());
            String[] parameterTypes = method.parameters();
            for (int i = 0; i < methodParameters.size(); i++) {
                VariableElement methodParameter = methodParameters.get(i);
                TypeMirror methodParameterType = methodParameter.asType();
                String t = methodParameterType.toString();

                if (methodParameterType instanceof TypeVariable) {
                    TypeVariable typeVariable = (TypeVariable) methodParameterType;
                    methodParameterType = typeVariable.getUpperBound();
                }

                for (int j = 0; j < parameterTypes.length; j++) {
                    if (methodParameterUsed.get(j)) {
                        continue;
                    }
                    if ((isSubtypeOfType(methodParameterType, parameterTypes[j])
                            && isSubtypeOfType(methodParameterType, VIEW_TYPE))
                            || isTypeEqual(methodParameterType, parameterTypes[j])
                            || isInterface(methodParameterType)) {
                        parameters[i] = new Parameter(j, TypeName.get(methodParameterType));
                        methodParameterUsed.set(j);
                        break;
                    }
                }
                if (parameters[i] == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Unable to match @")
                            .append(annotationClass.getSimpleName())
                            .append(" method arguments. (")
                            .append(enclosingElement.getQualifiedName())
                            .append('.')
                            .append(element.getSimpleName())
                            .append(')');
                    for (int j = 0; j < parameters.length; j++) {
                        Parameter parameter = parameters[j];
                        builder.append("\n\n  Parameter #")
                                .append(j + 1)
                                .append(": ")
                                .append(methodParameters.get(j).asType().toString())
                                .append("\n    ");
                        if (parameter == null) {
                            builder.append("did not match any listener parameters");
                        } else {
                            builder.append("matched listener parameter #")
                                    .append(parameter.getListenerPosition() + 1)
                                    .append(": ")
                                    .append(parameter.getType());
                        }
                    }
                    builder.append("\n\nMethods may have up to ")
                            .append(method.parameters().length)
                            .append(" parameter(s):\n");
                    for (String parameterType : method.parameters()) {
                        builder.append("\n  ").append(parameterType);
                    }
                    builder.append(
                            "\n\nThese may be listed in any order but will be searched for from top to bottom.");
                    error(executableElement, builder.toString());
                    return;
                }
            }
        }

        MethodViewBinding binding = new MethodViewBinding(name, Arrays.asList(parameters), true);
        BindingSet.Builder builder = getOrCreateBindingBuilder(builderMap, enclosingElement);
        for (String id : ids) {
            QualifiedId qualifiedId = elementToQualifiedId(element, id);
            if (!builder.addMethod(getId(qualifiedId), listener, method, binding)) {
                error(element, "Multiple listener methods with return value specified for ID %d. (%s.%s)",
                        id, enclosingElement.getQualifiedName(), element.getSimpleName());
                return;
            }
        }

        // Add the type-erased version to the valid binding targets set.
        erasedTargetNames.add(enclosingElement);


    }

    private void parseBindViews(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
                                Set<TypeElement> erasedTargetNames) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Start by verifying common generated code restrictions.
        boolean hasError = isInaccessibleViaGeneratedCode(BindViews.class, "fields", element)
                || isBindingInWrongPackage(BindViews.class, element);

        // Verify that the type is a List or an array.
        TypeMirror elementType = element.asType();
        String erasedType = doubleErasure(elementType);
        TypeMirror viewType = null;
        FieldCollectionViewBinding.Kind kind = null;
        if (elementType.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) elementType;
            viewType = arrayType.getComponentType();
            kind = FieldCollectionViewBinding.Kind.ARRAY;
        } else if (LIST_TYPE.equals(erasedType)) {
            DeclaredType declaredType = (DeclaredType) elementType;
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.size() != 1) {
                error(element, "@%s List must have a generic component. (%s.%s)",
                        BindViews.class.getSimpleName(), enclosingElement.getQualifiedName(),
                        element.getSimpleName());
                hasError = true;
            } else {
                viewType = typeArguments.get(0);
            }
            kind = FieldCollectionViewBinding.Kind.LIST;
        } else {
            error(element, "@%s must be a List or array. (%s.%s)", BindViews.class.getSimpleName(),
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }
        if (viewType != null && viewType.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) viewType;
            viewType = typeVariable.getUpperBound();
        }

        // Verify that the target type extends from View.
        if (viewType != null && !isSubtypeOfType(viewType, VIEW_TYPE) && !isInterface(viewType)) {
            if (viewType.getKind() == TypeKind.ERROR) {
                note(element, "@%s List or array with unresolved type (%s) "
                                + "must elsewhere be generated as a View or interface. (%s.%s)",
                        BindViews.class.getSimpleName(), viewType, enclosingElement.getQualifiedName(),
                        element.getSimpleName());
            } else {
                error(element, "@%s List or array type must extend from View or be an interface. (%s.%s)",
                        BindViews.class.getSimpleName(), enclosingElement.getQualifiedName(),
                        element.getSimpleName());
                hasError = true;
            }
        }

        // Assemble information on the field.
        String name = element.getSimpleName().toString();
        String[] ids = element.getAnnotation(BindViews.class).value();
        if (ids.length == 0) {
            error(element, "@%s must specify at least one ID. (%s.%s)", BindViews.class.getSimpleName(),
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }

        String duplicateId = findDuplicate(ids);
        if (duplicateId != null) {
            error(element, "@%s annotation contains duplicate ID %d. (%s.%s)",
                    BindViews.class.getSimpleName(), duplicateId, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        if (hasError) {
            return;
        }

        assert viewType != null; // Always false as hasError would have been true.
        TypeName type = TypeName.get(viewType);
        boolean required = isFieldRequired(element);

        List<Id> idVars = new ArrayList<>();
        for (String id : ids) {
            QualifiedId qualifiedId = elementToQualifiedId(element, id);
            idVars.add(getId(qualifiedId));
        }

        BindingSet.Builder builder = getOrCreateBindingBuilder(builderMap, enclosingElement);
        builder.addFieldCollection(new FieldCollectionViewBinding(name, type, kind, idVars, required));

        erasedTargetNames.add(enclosingElement);
    }

    private QualifiedId elementToQualifiedId(Element element, String id) {
        return new QualifiedId(elementUtils.getPackageOf(element).getQualifiedName().toString(), id);
    }

    /** Returns the first duplicate element inside an array, null if there are no duplicates. */
    private static String findDuplicate(String[] array) {
        Set<String> seenElements = new LinkedHashSet<>();

        for (String element : array) {
            if (!seenElements.add(element)) {
                return element;
            }
        }

        return null;
    }

    //判断typeMirror是否是otherType的子类
    static boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        //如果类型一样返回true
        if (isTypeEqual(typeMirror, otherType)) {
            return true;
        }
        //如果类型不是class或者interface则返回false
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        //泛型的处理
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        //返回此类型元素的完全限定名称。
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        // 获取变量修饰符
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }


    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }
}
