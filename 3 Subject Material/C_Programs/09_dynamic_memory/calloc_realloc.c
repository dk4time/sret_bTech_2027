/*
 * Program 9.2 - calloc vs malloc, and realloc
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>
#include <stdlib.h>

int main(void) {
    int *a = calloc(3, sizeof(int));          /* all zeros */
    if (a == NULL) return 1;
    printf("After calloc : %d %d %d\n", a[0], a[1], a[2]);

    a[0] = 1; a[1] = 2; a[2] = 3;
    int *tmp = realloc(a, 5 * sizeof(int));
    if (tmp == NULL) { free(a); return 1; }
    a = tmp;
    a[3] = 4; a[4] = 5;

    printf("After realloc:");
    for (int i = 0; i < 5; i++) printf(" %d", a[i]);
    printf("\n");
    free(a);
    return 0;
}
