/*
 * Program 4.2 - Second largest in one pass
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>
#include <limits.h>

int main(void) {
    int a[] = {12, 35, 1, 10, 34, 35};
    int n = sizeof(a) / sizeof(a[0]);
    int first = INT_MIN, second = INT_MIN;

    for (int i = 0; i < n; i++) {
        if (a[i] > first) {
            second = first;
            first = a[i];
        } else if (a[i] > second && a[i] != first) {
            second = a[i];
        }
    }
    printf("Largest = %d, second largest = %d\n", first, second);
    return 0;
}
