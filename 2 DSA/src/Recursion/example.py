def display(num):#
    if num == 0: #base condition
        return 5
    print(num, end=" ") #task
    display(num-1) #recursion call-iterating
    print(num, end=" ")  # task
print(display(5))

def fact(n):
    if n == 0:
        return 1
    return n * fact(n-1)
fact(5)

def fib(n):
    if n == 0: return 0
    if n == 1: return 1
    return fib(n-1) + fib(n-2)



fib(5)
