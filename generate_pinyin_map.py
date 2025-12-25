import urllib.request
import unicodedata
import re
from collections import defaultdict

def strip_accents(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s)
                   if unicodedata.category(c) != 'Mn')

def generate_mapping():
    url = "https://raw.githubusercontent.com/mozillazg/pinyin-data/master/pinyin.txt"
    print(f"Downloading {url}...")
    
    with urllib.request.urlopen(url) as response:
        data = response.read().decode('utf-8')

    pinyin_map = defaultdict(set)
    
    # Common characters range (roughly)
    # To keep it reasonable, we might basic filtering, but let's see size first.
    # Actually, let's just process all line by line.
    
    lines = data.split('\n')
    for line in lines:
        line = line.strip()
        if not line or line.startswith('#'):
            continue
            
        # Format: U+3007: líng,yuán,xīng  # 〇
        parts = line.split('#')
        if len(parts) < 2:
            continue
            
        content = parts[0].strip()
        character = parts[1].strip()
        
        # Filter for Simplified Chinese (GB2312)
        # Verify if the character is in GB2312 charset
        try:
            character.encode('gb2312')
        except UnicodeEncodeError:
            continue

        # Extract pinyin part: U+3007: líng,yuán,xīng
        if ':' not in content:
            continue
            
        pinyin_raw = content.split(':')[1].strip()
        pinyins = pinyin_raw.split(',')
        
        for p in pinyins:
            p = p.strip()
            if not p:
                continue
            
            # Strip tones
            p_clean = strip_accents(p).lower()
            # Remove any non-alphabetic chars just in case
            p_clean = re.sub(r'[^a-z]', '', p_clean)
            
            if p_clean:
                pinyin_map[p_clean].add(character)

    print(f"Processed {len(pinyin_map)} unique pinyin syllables.")
    
    # Sort keys
    sorted_pinyins = sorted(pinyin_map.keys())
    
    output_lines = []
    for p in sorted_pinyins:
        chars = sorted(list(pinyin_map[p]))
        # Join into: pinyin char1 char2 ...
        line = f"{p} {' '.join(chars)}"
        output_lines.append(line)
        
    with open('pinyin_mapping.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))
        
    print("Done. Saved to pinyin_mapping.txt")

if __name__ == "__main__":
    generate_mapping()
