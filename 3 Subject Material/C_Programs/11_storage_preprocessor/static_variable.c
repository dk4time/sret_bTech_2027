/*
 * Program 11.1 - static local variable
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

void visit(void) {
    static int count = 0;
    int normal = 0;
    count++;
    normal++;
    printf("static = %d, normal = %d\n", count, normal);
}

int main(void) {
    visit();
    visit();
    visit();
    return 0;
}
