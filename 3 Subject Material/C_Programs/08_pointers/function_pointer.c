/*
 * Program 8.3 - Function pointers
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int add(int a, int b) { return a + b; }
int sub(int a, int b) { return a - b; }
int mul(int a, int b) { return a * b; }

int main(void) {
    int (*ops[3])(int, int) = {add, sub, mul};
    const char *names[3] = {"add", "sub", "mul"};

    for (int i = 0; i < 3; i++)
        printf("%s(8, 3) = %d\n", names[i], ops[i](8, 3));
    return 0;
}
