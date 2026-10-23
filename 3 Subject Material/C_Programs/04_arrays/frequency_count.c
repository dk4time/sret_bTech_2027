/*
 * Program 4.4 - Frequency of each element
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[] = {4, 2, 4, 5, 2, 4};
    int n = 6, visited[6] = {0};

    for (int i = 0; i < n; i++) {
        if (visited[i]) continue;
        int count = 1;
        for (int j = i + 1; j < n; j++) {
            if (a[j] == a[i]) {
                count++;
                visited[j] = 1;
            }
        }
        printf("%d occurs %d time(s)\n", a[i], count);
    }
    return 0;
}
