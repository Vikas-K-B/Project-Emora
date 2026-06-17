with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    content = f.read()

open_b = content.count('{')
close_b = content.count('}')
print(f'Open brackets: {open_b}')
print(f'Close brackets: {close_b}')
