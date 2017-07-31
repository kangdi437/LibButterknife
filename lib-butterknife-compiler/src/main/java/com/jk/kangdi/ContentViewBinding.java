package com.jk.kangdi;

import com.squareup.javapoet.CodeBlock;

/**
 * Created by JINKANG on 2017/7/27.
 */

public class ContentViewBinding {

    String name;
    String value;

    public ContentViewBinding(String name, String value) {
        this.name = name;
        this.value = value;
    }

    CodeBlock render(String moduleName){
        return CodeBlock.builder()
            .add("target.setContentView($L.$L)" , moduleName , value).build();
    }


}
