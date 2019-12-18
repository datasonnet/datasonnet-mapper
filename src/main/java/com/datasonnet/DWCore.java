package com.datasonnet;

import sjsonnet.Val;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DWCore {

    public static Boolean isInteger(Val value){
        if(value.prettyName() == "string"){
            try {
                Integer.parseInt(((Val.Str)value).value());
            } catch(NumberFormatException e) {
                return false;
            }
            return true;
        }
        else if(value.prettyName() == "number"){
            double temp = ((Val.Num) value).value();
            if(temp == (int) temp){
                return true;
            }
            return false;
        }
        return false;
    }


    public static String match(String str, String regex) {

        String content = "[";

        // Create a Pattern object
        Pattern r = Pattern.compile(regex);

        // Now create matcher object.
        Matcher m = r.matcher(str);
        while(m.find()) {
            for(int i=0; i<m.groupCount(); i++) {
                content += "\"" + m.group(i) +"\",";
            }
        }
        return content.substring(0,content.length()-1) + "]";
    }

    public static Boolean matches(String str, String regex){
        return str.matches(regex);
    }

    public static String replace(String str, String regex, String replacement) {


        // Create a Pattern object
        Pattern r = Pattern.compile(regex);

        // Now create matcher object.
        Matcher m = r.matcher(str);
        while(m.find()) {
            return "\"" + m.replaceAll(replacement) +"\"";
        }
        return "\"" + str +"\"";
    }

    public static String uuid(){
        int n = 36;
        // chose a Character random from this String
        String AlphaNumericString = "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            if(i == 8 || i == 13 || i == 18 || i == 23){
                sb.append('-');
            }
            else {
                int index
                        = (int) (AlphaNumericString.length()
                        * Math.random());
                sb.append(AlphaNumericString
                        .charAt(index));
            }
        }
        return "\"" + sb.toString() + "\"";
    }
}
