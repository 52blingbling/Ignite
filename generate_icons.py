import os
from PIL import Image, ImageDraw, ImageFont

def create_icon(size, text="启程", font_path="C:\\Windows\\Fonts\\simkai.ttf", is_round=False):
    # Create white background
    img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    if is_round:
        draw.ellipse((0, 0, size, size), fill=(255, 255, 255, 255))
    else:
        draw.rectangle((0, 0, size, size), fill=(255, 255, 255, 255))
    
    try:
        # We need the text to fit the icon, maybe about 60% of the size
        font_size = int(size * 0.45) 
        font = ImageFont.truetype(font_path, font_size)
    except IOError:
        print(f"Font not found at {font_path}, using default.")
        font = ImageFont.load_default()
        
    # Calculate text bounding box
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    x = (size - text_width) / 2
    # Adjust y to be visually centered, taking into account font baseline
    y = (size - text_height) / 2 - bbox[1] 
    
    # Draw black text
    draw.text((x, y), text, font=font, fill=(0, 0, 0, 255))
    
    return img

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

base_dir = "e:\\python program\\Ignite\\app\\src\\main\\res"

for density, size in sizes.items():
    density_dir = os.path.join(base_dir, f"mipmap-{density}")
    if not os.path.exists(density_dir):
        os.makedirs(density_dir)
        
    # Standard icon
    icon = create_icon(size)
    icon.save(os.path.join(density_dir, "ic_launcher.png"))
    
    # Round icon
    icon_round = create_icon(size, is_round=True)
    icon_round.save(os.path.join(density_dir, "ic_launcher_round.png"))

print("Icons generated successfully!")
