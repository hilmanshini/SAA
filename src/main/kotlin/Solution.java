import java.io.*;

import static java.util.stream.Collectors.joining;
import java.util.Arrays;

class Solution {
    public int solution(int[] A) {
        final long MOD = 1_000_000_000L;
        long profit = 0;
        int n = A.length;

        int i = 0;
        while (i < n - 1) {
            // Find local minimum (valley) — a buying point
            while (i < n - 1 && A[i] >= A[i + 1]) i++;
            if (i == n - 1) break;
            int buy = A[i];
            i++;

            // Find local maximum (peak) — a selling point
            while (i < n && (i == n - 1 || A[i] >= A[i - 1])) i++;
            int sell = A[i - 1];

            profit = (profit + (sell - buy)) % MOD;
        }

        return (int) profit;
    }

    public int[] getSums(int[] A) {
        int length = A.length;
        int[] sums = new int[length - 1];
        for (int i = 0; i < length - 1; i++) {
            sums[i] = A[i] + A[i + 1];
        }
        return sums;
    }

    public int threeCombinations(int length, int[] sums) {
        int result = 0;
        for (int i = 0; i < length - 2; i++) {
            for (int j = i + 1; j < length - 1; j++) {
                for (int k = j + 1; k < length; k++) {
                    if (isValid(i, j, k, length)) {
                        result = Math.max(result, sums[i] + sums[j] + sums[k]);
                    }
                }
            }
        }
        return result;
    }

    public int twoCombinations(int start, int length, int[] sums) {
        int result = 0;
        for (int i = start; i < length -1; i++) {
            for (int j = i + 1; j < length; j++) {
                if (isValid(i, j, -1, length)) {
                    result = Math.max(result, sums[i] + sums[j]);
                }
            }
        }
        if (length > 0) {
            for (int i = start; i < length -1; i++) {
                result = Math.max(result, sums[i]);
            }
        }
        return result;

    }

    public boolean isValid(int i, int j, int k, int length) {
        if (k == -1) {
            return j - i > 1;

        }
        return (j - i > 1) && (k - j > 1);
    }


    public static void main(String[] args) {
        Solution sol = new Solution();
        int[] A1 = {2, 3, 5, 2, 3, 4, 6, 4, 1};
        System.out.println(sol.solution(A1)); //25

        int[] A2 = {1, 5, 3, 2, 6, 6, 10, 4, 7, 2, 1};
        System.out.println(sol.solution(A2)); //35

        int[] A3 = {1, 2, 3, 3, 2};
        System.out.println(sol.solution(A3)); //10

        int[] A4 = {5, 10, 3};
        System.out.println(sol.solution(A4)); //15

        int[] A5 = {1,2};
        System.out.println(sol.solution(A5)); //3

        int[] A6 = {1};
        System.out.println(sol.solution(A6)); //0

        int [] A7 = {};
        System.out.println(sol.solution(A7)); //0

    }
}