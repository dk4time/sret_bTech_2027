/*
 * Program 4.6 - Merge two sorted arrays
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[] = {1, 4, 7, 9}, b[] = {2, 3, 8};
    int n = 4, m = 3, c[7];
    int i = 0, j = 0, k = 0;

    while (i < n && j < m)
        c[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
    while (i < n) c[k++] = a[i++];
    while (j < m) c[k++] = b[j++];

    for (int x = 0; x < k; x++) printf("%d ", c[x]);
    printf("\n");
    return 0;
}
