import argparse
import functools
import math
import os
import random


parser = argparse.ArgumentParser()
parser.add_argument('command', nargs='*', choices=['train', 'test'])
parser.add_argument("--clf", action="store_true", default=False, help='Only create classification data')
parser.add_argument("--reg", action="store_true", default=False, help='Only create regression data')
args = parser.parse_args()

TARGET_DIR = os.path.dirname(__file__)

sign = functools.partial(math.copysign, 1)

random.seed(1337)

suffixList = ['clf', 'reg']
if args.clf:
    suffixList = ['clf']
if args.reg:
    suffixList = ['reg']

for command in args.command:
    for suff in suffixList:

        if command == 'train':
            nInstances = 1000
        elif command == 'test':
            nInstances = 1000
        else:
            nInstances = 1000

        filename = os.path.join(TARGET_DIR, command + '_' + suff + '.csv')
        print('Creating data in', filename)

        with open(filename, 'w') as myfile:

            if suff == 'clf':
                print('Index X Y Type', file=myfile)

                for i in range(nInstances):
                    label = random.randint(0, 1)
                    x = 0
                    y = 0
                    strategy = 'diagonal'
                    if strategy == 'halfs':
                        x = -random.random() if label is 0 else random.random()
                        y = 2 * random.random() - 1
                    elif strategy == 'quarters':
                        x = 2 * random.random() - 1
                        y = (1 if label == 1 else -1) * sign(x) * random.random()
                    elif strategy == 'diagonal':
                        x = 2 * random.random() - 1
                        if label == 1:
                            y = x + random.random() * (1 - x)
                        else:
                            y = random.random() * (1 + x) - 1
                    elif strategy == 'circle':
                        x = 2 * random.random() - 1
                        y = 2 * random.random() - 1
                        label = 1 if math.sqrt(x**2 + y**2) < 0.5 else 0
                    x = round(x, 2)
                    y = round(y, 2)
                    print('{} {} {} {}'.format(i, x, y, label), file=myfile)

            elif suff == 'reg':
                print('Index X Y', file=myfile)

                for i in range(nInstances):
                    x = -1 + i * 2 / (nInstances - 1)
                    y = 0
                    strategy = 'linear'
                    if strategy == 'linear':
                        y = 2 * x + 1

                    x = round(x, 3)
                    y = round(y, 3)
                    print('{} {} {}'.format(i, x, y), file=myfile)
