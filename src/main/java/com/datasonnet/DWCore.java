package com.datasonnet;

import sjsonnet.Val;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DWCore {


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
