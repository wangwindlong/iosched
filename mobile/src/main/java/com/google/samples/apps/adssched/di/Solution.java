package com.google.samples.apps.adssched.di;

import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Solution {
    public static int firstMissingPositive(int[] nums) {
        int result = 1;
        Arrays.sort(nums);
        int last = 0;
        for (int num : nums) {
            if (num > result || last == num || num <= 0) continue;
            last = num;
            result += 1;
        }
        return result;
    }

    public static int getMin(int[] nums, int d) {
        int m = 0;
        for (int num : nums) {
            if (num < m && num > d) m = num;
        }
        return m;
    }


    public static void main(String[] args) {
        System.out.println("result=" + firstMissingPositive(new int[]{2000, 0, 1, 1, 50, 1000}));
    }
}
