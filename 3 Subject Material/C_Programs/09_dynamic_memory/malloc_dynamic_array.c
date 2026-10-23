/*
 * Program 9.1 - Dynamic array with malloc
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>
#include <stdlib.h>

int main(void) {
    int n = 5, sum = 0;
    int *arr = malloc(n * sizeof(int));
    if (arr == NULL) {
        printf("Allocation failed\n");
        return 1;
    }
    for (int i = 0; i < n; i++) {
        arr[i] = (i + 1) * (i + 1);
        sum += arr[i];
    }
    printf("Squares sum = %d\n", sum);
    free(arr);
    arr = NULL;
    return 0;
}
