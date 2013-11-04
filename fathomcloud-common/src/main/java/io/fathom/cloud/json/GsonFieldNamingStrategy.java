package io.fathom.cloud.json;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

public class GsonFieldNamingStrategy implements FieldNamingStrategy {

    @Override
    public String translateName(Field f) {
        String name = null;
        XmlElement xmlElement = f.getAnnotation(XmlElement.class);
        if (xmlElement != null) {
            name = xmlElement.name();
            if (name.equals("##default")) {
                name = null;
            }
        } else {
            XmlAttribute xmlAttribute = f.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null) {
                name = xmlAttribute.name();
                if (name.equals("##default")) {
                    name = null;
                }
            }
        }

        if (name == null) {
            name = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(f);
        }

        return name;
    }

}
