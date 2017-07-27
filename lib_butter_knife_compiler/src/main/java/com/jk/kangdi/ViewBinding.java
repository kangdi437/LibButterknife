package com.jk.kangdi;


import com.jk.kangdi.internal.ListenerClass;
import com.jk.kangdi.internal.ListenerMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by JINKANG on 2017/7/25.
 */

final class ViewBinding {

    private final Id id;
    private final Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> methodBindings;
    private final FieldViewBinding fieldBinding;

    ViewBinding(Id id, Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> methodBindings,
                FieldViewBinding fieldBinding) {
        this.id = id;
        this.methodBindings = methodBindings;
        this.fieldBinding = fieldBinding;
    }
    public Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> getMethodBindings() {
        return methodBindings;
    }
    public FieldViewBinding getFieldBinding() {
        return fieldBinding;
    }

    public boolean requiresLocal() {
        if (isBoundToRoot()) {
            return false;
        }
        if (isSingleFieldBinding()) {
            return false;
        }
        return true;
    }

    public List<MemberViewBinding> getRequiredBindings() {
        List<MemberViewBinding> requiredBindings = new ArrayList<>();
        if (fieldBinding != null && fieldBinding.isRequired()) {
            requiredBindings.add(fieldBinding);
        }
        for (Map<ListenerMethod, Set<MethodViewBinding>> methodBinding : methodBindings.values()) {
            for (Set<MethodViewBinding> set : methodBinding.values()) {
                for (MethodViewBinding binding : set) {
                    if (binding.isRequired()) {
                        requiredBindings.add(binding);
                    }
                }
            }
        }
        return requiredBindings;
    }

    public boolean isSingleFieldBinding() {
        return methodBindings.isEmpty() && fieldBinding != null;
    }

    public Id getId() {
        return id;
    }

    public boolean isBoundToRoot() {
        return LibButterknifeProcessor.NO_ID.equals(id);
    }

    public static final class Builder {
        private final Id id;

        private final Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> methodBindings =
                new LinkedHashMap<>();

        FieldViewBinding fieldBinding;

        Builder(Id id) {
            this.id = id;
        }

        public void setFieldBinding(FieldViewBinding fieldBinding) {
            if (this.fieldBinding != null) {
                throw new AssertionError();
            }
            this.fieldBinding = fieldBinding;
        }

        public ViewBinding build() {
            return new ViewBinding(id, methodBindings, fieldBinding);
        }

    }

}
