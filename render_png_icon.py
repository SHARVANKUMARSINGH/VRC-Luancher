#!/usr/bin/env python3
import zlib
import struct
import math
import os

def create_png(filename, width, height, pixel_fn):
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # Filter byte 0 (None)
        for x in range(width):
            r, g, b, a = pixel_fn(x, y, width, height)
            raw.extend([
                max(0, min(255, int(r))),
                max(0, min(255, int(g))),
                max(0, min(255, int(b))),
                max(0, min(255, int(a)))
            ])

    def chunk(chunk_type, data):
        return struct.pack(">I", len(data)) + chunk_type + data + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xffffffff)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    compressed = zlib.compress(raw, 9)
    png_data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")

    with open(filename, "wb") as f:
        f.write(png_data)
    print(f"Generated {filename} ({width}x{height})")

def render_vrc_icon(x, y, w, h):
    # Normalized coordinates [-1, 1]
    nx = (x / (w - 1)) * 2.0 - 1.0
    ny = (y / (h - 1)) * 2.0 - 1.0

    # Squircle distance: (|x|^4 + |y|^4)^(1/4)
    # Scaled to fit comfortably inside canvas
    sx = abs(nx) / 0.88
    sy = abs(ny) / 0.88
    s_dist = (sx**4 + sy**4)**0.25

    # Outside the squircle
    if s_dist > 1.02:
        return (0, 0, 0, 0)

    # Edge antialiasing for squircle
    edge_alpha = 1.0
    if s_dist > 0.98:
        edge_alpha = (1.02 - s_dist) / 0.04

    # Outer border highlight
    is_border = (s_dist >= 0.94)

    # Base background gradient (radial/linear hybrid from top-left to bottom-right)
    grad_t = 0.5 * (nx * 0.3 - ny * 0.7 + 1.0)
    # Dark modern surface: #1a1e27 to #0e1014
    bg_r = 26 * (1 - grad_t) + 14 * grad_t
    bg_g = 30 * (1 - grad_t) + 16 * grad_t
    bg_b = 39 * (1 - grad_t) + 20 * grad_t

    if is_border:
        # Subtle glowing metallic rim with Google Blue tint
        return (66, 133, 244, int(255 * edge_alpha * 0.85))

    # Center is at (0, -0.05)
    cx = nx
    cy = ny + 0.05

    # 3D Isometric Cube definition in normalized coords
    # Center vertex: (0, 0)
    # Top vertex: (0, -0.42)
    # Bottom vertex: (0, 0.42)
    # Left vertex: (-0.42, -0.15)
    # Right vertex: (0.42, -0.15)
    # Bottom-Left vertex: (-0.42, 0.27)
    # Bottom-Right vertex: (0.42, 0.27)

    col_r, col_g, col_b = bg_r, bg_g, bg_b

    # Helper: point in triangle
    def pt_in_tri(px, py, ax, ay, bx, by, cx, cy):
        d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
        d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
        d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
        has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
        has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
        return not (has_neg and has_pos)

    # Top Face: (0, -0.42), (-0.42, -0.18), (0, 0.06), (0.42, -0.18)
    in_top_1 = pt_in_tri(cx, cy, 0.0, -0.42, -0.42, -0.18, 0.0, 0.06)
    in_top_2 = pt_in_tri(cx, cy, 0.0, -0.42, 0.42, -0.18, 0.0, 0.06)
    in_top = in_top_1 or in_top_2

    # Left Face: (-0.42, -0.18), (0, 0.06), (0, 0.46), (-0.42, 0.22)
    in_left_1 = pt_in_tri(cx, cy, -0.42, -0.18, 0.0, 0.06, 0.0, 0.46)
    in_left_2 = pt_in_tri(cx, cy, -0.42, -0.18, 0.0, 0.46, -0.42, 0.22)
    in_left = in_left_1 or in_left_2

    # Right Face: (0.42, -0.18), (0, 0.06), (0, 0.46), (0.42, 0.22)
    in_right_1 = pt_in_tri(cx, cy, 0.42, -0.18, 0.0, 0.06, 0.0, 0.46)
    in_right_2 = pt_in_tri(cx, cy, 0.42, -0.18, 0.0, 0.46, 0.42, 0.22)
    in_right = in_right_1 or in_right_2

    if in_top:
        # Shaded top face (slate gray / obsidian highlights #4C566A)
        col_r, col_g, col_b = 76, 86, 106
    elif in_left:
        # Darker left face #3B4252
        col_r, col_g, col_b = 59, 66, 82
    elif in_right:
        # Deepest right face #2E3440
        col_r, col_g, col_b = 46, 52, 64

    # Glowing "V" Emblem in the center of the block
    # Polygon points: (-0.18, -0.12) -> (0, 0.22) -> (0.18, -0.12) -> (0.10, -0.12) -> (0, 0.10) -> (-0.10, -0.12)
    # Check if point inside V
    v_active = False
    if cy >= -0.14 and cy <= 0.24 and abs(cx) <= 0.22:
        # Left leg of V
        line_out = (cy + 0.12) / 0.34 * 0.18 # x width at height cy
        x_dist = abs(cx)
        target_x = 0.18 * ((0.22 - cy) / 0.34)
        if abs(x_dist - target_x) < 0.045 and cy <= 0.20:
            v_active = True
        elif cy > 0.18 and abs(cx) < 0.045:
            v_active = True

    if v_active:
        # Neon Google Blue (#4285F4)
        col_r, col_g, col_b = 66, 133, 244

    # MCPE Square Joystick Graphic in Bottom-Left:
    # center around (-0.50, 0.52)
    jx = nx - (-0.48)
    jy = ny - 0.48
    if abs(jx) <= 0.16 and abs(jy) <= 0.16:
        # Outer square border
        if abs(jx) >= 0.14 or abs(jy) >= 0.14:
            col_r, col_g, col_b = 66, 133, 244 # Google blue border
        elif abs(jx) <= 0.07 and abs(jy) <= 0.07:
            # Knob in center
            if abs(jx) <= 0.02 and abs(jy) <= 0.02:
                col_r, col_g, col_b = 66, 133, 244
            else:
                col_r, col_g, col_b = 45, 52, 66

    # Google 4-Color Quad in Bottom-Right:
    # 4 circles around (0.48, 0.48)
    # Blue: (0.42, 0.42), Red: (0.54, 0.42), Yellow: (0.42, 0.54), Green: (0.54, 0.54)
    r_dot = 0.038
    d_blue = math.hypot(nx - 0.42, ny - 0.42)
    d_red = math.hypot(nx - 0.54, ny - 0.42)
    d_yellow = math.hypot(nx - 0.42, ny - 0.54)
    d_green = math.hypot(nx - 0.54, ny - 0.54)

    if d_blue <= r_dot:
        col_r, col_g, col_b = 66, 133, 244 # Blue
    elif d_red <= r_dot:
        col_r, col_g, col_b = 234, 67, 53 # Red
    elif d_yellow <= r_dot:
        col_r, col_g, col_b = 251, 188, 5 # Yellow
    elif d_green <= r_dot:
        col_r, col_g, col_b = 52, 168, 83 # Green

    return (col_r, col_g, col_b, int(255 * edge_alpha))

def main():
    # 1. Generate Favicon (64x64, 128x128)
    create_png("favicon.png", 128, 128, render_vrc_icon)
    create_png("favicon-32x32.png", 32, 32, render_vrc_icon)
    create_png("favicon-16x16.png", 16, 16, render_vrc_icon)

    # 2. Generate HD Logo (512x512)
    create_png("logo.png", 512, 512, render_vrc_icon)
    create_png("vrc_launcher.png", 512, 512, render_vrc_icon)

    # 3. Copy to assets and drawable
    os.system("cp logo.png Vrc/src/main/assets/logo.png")
    os.system("cp favicon.png Vrc/src/main/assets/favicon.png")
    os.system("cp logo.png Vrc/src/main/res/drawable/img_vrc_logo.png")
    os.system("cp logo.png Vrc/src/main/res/drawable/img_vrc_emblem.png")
    os.system("cp logo.png Vrc/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
    os.system("cp logo.png Vrc/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png")

    print("All PNG icons and favicons created and deployed successfully!")

if __name__ == "__main__":
    main()
