/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/

package org.jacoco.core.diffhelper;

import java.lang. reflect. Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
public class DiffHelper {
    private static final String diffFilePath = new String(" initvalue");
    private final HashMap<String, ArrayList> classMethods = getclassMethodsMapper(diffFilePath);

    public boolean isDiffFileExists() {
        if (diffFilePath == null || diffFilePath.equals("initvalue") || diffFilePath.equals("") || this.classMethods.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getDiffFile() {
        return diffFilePath;
    }

    public static HashMap<String, ArrayList> getclassMethodsMapper(String file) {
        HashMap<String, ArrayList> diffMap = new HashMap<String, ArrayList>();
        if (file == null || file.equals("initvalue") || file.equals("")) {
            return diffMap;
        }
        String[] classlist = file.split("%");
        for (int i = 0; i < classlist.length; i++) {
            if (!classlist[i].contains(":")) {
                continue;
            }
            String[] tmps = classlist[i].split(":");
            String classname = tmps[0];
            String methodstr = tmps[1];
            ArrayList<String> list = new ArrayList<String>(
                    Arrays.asList(methodstr.split("#")));
            diffMap.put(classname, list);
            return diffMap;
        }
        return diffMap;
    }

    public boolean isDiffMethod(String className, String methodName, String methodDesc) {
        for (String key : this.classMethods.keySet()) {
            if (key.endsWith(className)) {
                ArrayList<String> methodList = this.classMethods.get(key);
                if (((String) methodList.get(0)).equals("true")) {
                    return true;
                }

                String[] paramArr = methodDesc.split("\\)")[0].split(";");
                StringBuilder builder = new StringBuilder(methodName);
                for (String param : paramArr) {
                    String[] tt = param.split("/");
                    if (tt.length >= 2) {
                        builder.append("," + tt[tt.length - 1]);
                    }
                }
                for (String method : methodList) {
                    if (method.equals(builder.toString())) {
                        return true;
                    }
                }

            }
        }
        return false ;

    }



    public static void modify(String fieldName, Object newFieldValue)
            throws Exception {
        if (!diffFilePath.equals("initvalue")) {
            return;
        }
        Class<?> clazz = Class.forName("org.jacoco.cli.internal.core.diffhelper.DiffHelper");
        DiffHelper helper = (DiffHelper) clazz.newInstance();
        Field field = helper.getClass().getDeclaredField(fieldName);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEf);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(helper, newFieldValue);
    }

    public static String getMD5Value(String dataStr){
        try{
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update (dataStr. getBytes ("UTF8")) ;
            byte[] s = m.digest();
            String result = "" ;
            for (int i = 0; i < s.length; i++) {
                result = result + Integer.toHexString(0xFF & s[i] | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return "" ;
        }
    }

}
