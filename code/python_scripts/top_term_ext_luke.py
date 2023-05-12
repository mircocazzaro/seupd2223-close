#searches in luke comma separated output and print out first k terms ordered by tf desc

from operator import itemgetter

filename = "terms.out"  # replace with your file name
k = 1200
arr = []

with open(filename, 'r', encoding="utf-8") as file:
    for line in file:
        data = line.split(',')
        try:
            arr.append([data[0],int(data[1])])
        except ValueError:
            pass


arr.sort(key=itemgetter(1), reverse = True)

for i in range(0, k):
    print(arr[i][0])

