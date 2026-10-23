/*
 * Program 10.1 - Array of structures - find the topper
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

struct Student {
    char name[20];
    int marks;
};

int main(void) {
    struct Student s[3] = {{"Asha", 82}, {"Ravi", 91}, {"Meena", 77}};
    int top = 0;
    for (int i = 1; i < 3; i++)
        if (s[i].marks > s[top].marks) top = i;
    printf("Topper: %s with %d marks\n", s[top].name, s[top].marks);
    return 0;
}
