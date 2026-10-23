/*
 * Program 4.3 - Rotate an array left by k
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

void reverse(int a[], int l, int r) {
    while (l < r) {
        int t = a[l]; a[l] = a[r]; a[r] = t;
        l++; r--;
    }
}

void rotateLeft(int a[], int n, int k) {
    k %= n;
    reverse(a, 0, k - 1);
    reverse(a, k, n - 1);
    reverse(a, 0, n - 1);
}

int main(void) {
    int a[] = {1, 2, 3, 4, 5, 6, 7};
    int n = 7;
    rotateLeft(a, n, 2);
    for (int i = 0; i < n; i++) printf("%d ", a[i]);
    printf("\n");
    return 0;
}
