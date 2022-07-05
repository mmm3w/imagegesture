import sys
import io

targetRead = sys.argv[1]
implementation = sys.argv[2]
key = sys.argv[3]

newContent = ''
with io.open(targetRead, 'r', encoding='utf-8') as f:
    for line in f:
        if key in line:
            newContent += implementation + '\n'
        else:
            newContent += line

with io.open(targetRead, 'w', encoding='utf-8') as f:
    f.write(newContent)
