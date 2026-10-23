/*
 * Program 4.5 - Remove duplicates from a sorted array
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int removeDuplicates(int a[], int n) {
    if (n == 0) return 0;
    int k = 1;
    for (int i = 1; i < n; i++)
        if (a[i] != a[k - 1])
            a[k++] = a[i];
    return k;
}

int main(void) {
    int a[] = {1, 1, 2, 3, 3, 3, 4};
    int k = removeDuplicates(a, 7);
    printf("Unique count = %d: ", k);
    for (int i = 0; i < k; i++) printf("%d ", a[i]);
    printf("\n");
    return 0;
}
