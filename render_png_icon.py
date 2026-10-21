#!/usr/bin/env python3
import zlib
import struct
import math
import os

def create_png(filename, width, height, pixel_fn):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
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

    os.makedirs(os.path.dirname(os.path.abspath(filename)), exist_ok=True)
    with open(filename, "wb") as f:
        f.write(png_data)
    print(f"Generated {filename} ({width}x{height})")

def pt_in_tri(px, py, ax, ay, bx, by, cx, cy):
    d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
    d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
    d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)

def render_content(nx, ny, bg_r, bg_g, bg_b):
    cx = nx
    cy = ny + 0.05
    col_r, col_g, col_b = bg_r, bg_g, bg_b

    in_top_1 = pt_in_tri(cx, cy, 0.0, -0.42, -0.42, -0.18, 0.0, 0.06)
    in_top_2 = pt_in_tri(cx, cy, 0.0, -0.42, 0.42, -0.18, 0.0, 0.06)
    in_top = in_top_1 or in_top_2

    in_left_1 = pt_in_tri(cx, cy, -0.42, -0.18, 0.0, 0.06, 0.0, 0.46)
    in_left_2 = pt_in_tri(cx, cy, -0.42, -0.18, 0.0, 0.46, -0.42, 0.22)
    in_left = in_left_1 or in_left_2

    in_right_1 = pt_in_tri(cx, cy, 0.42, -0.18, 0.0, 0.06, 0.0, 0.46)
    in_right_2 = pt_in_tri(cx, cy, 0.42, -0.18, 0.0, 0.46, 0.42, 0.22)
    in_right = in_right_1 or in_right_2

    is_cube = in_top or in_left or in_right
    if in_top:
        col_r, col_g, col_b = 76, 86, 106
    elif in_left:
        col_r, col_g, col_b = 59, 66, 82
    elif in_right:
        col_r, col_g, col_b = 46, 52, 64

    # Glowing V
    v_active = False
    if cy >= -0.14 and cy <= 0.24 and abs(cx) <= 0.22:
        target_x = 0.18 * ((0.22 - cy) / 0.34)
        if abs(abs(cx) - target_x) < 0.045 and cy <= 0.20:
            v_active = True
        elif cy > 0.18 and abs(cx) < 0.045:
            v_active = True

    if v_active:
        col_r, col_g, col_b = 66, 133, 244
        is_cube = True

    # Joystick
    jx = nx - (-0.48)
    jy = ny - 0.48
    is_joy = False
    if abs(jx) <= 0.16 and abs(jy) <= 0.16:
        is_joy = True
        if abs(jx) >= 0.14 or abs(jy) >= 0.14:
            col_r, col_g, col_b = 66, 133, 244
        elif abs(jx) <= 0.07 and abs(jy) <= 0.07:
            if abs(jx) <= 0.02 and abs(jy) <= 0.02:
                col_r, col_g, col_b = 66, 133, 244
            else:
                col_r, col_g, col_b = 45, 52, 66

    # Google 4-Color Quad
    r_dot = 0.038
    d_blue = math.hypot(nx - 0.42, ny - 0.42)
    d_red = math.hypot(nx - 0.54, ny - 0.42)
    d_yellow = math.hypot(nx - 0.42, ny - 0.54)
    d_green = math.hypot(nx - 0.54, ny - 0.54)
    is_dots = False

    if d_blue <= r_dot:
        col_r, col_g, col_b = 66, 133, 244
        is_dots = True
    elif d_red <= r_dot:
        col_r, col_g, col_b = 234, 67, 53
        is_dots = True
    elif d_yellow <= r_dot:
        col_r, col_g, col_b = 251, 188, 5
        is_dots = True
    elif d_green <= r_dot:
        col_r, col_g, col_b = 52, 168, 83
        is_dots = True

    return (col_r, col_g, col_b, is_cube or is_joy or is_dots)

def render_vrc_icon(x, y, w, h):
    nx = (x / (w - 1)) * 2.0 - 1.0
    ny = (y / (h - 1)) * 2.0 - 1.0

    sx = abs(nx) / 0.88
    sy = abs(ny) / 0.88
    s_dist = (sx**4 + sy**4)**0.25

    if s_dist > 1.02:
        return (0, 0, 0, 0)

    edge_alpha = 1.0
    if s_dist > 0.98:
        edge_alpha = (1.02 - s_dist) / 0.04

    is_border = (s_dist >= 0.94)

    grad_t = 0.5 * (nx * 0.3 - ny * 0.7 + 1.0)
    bg_r = 26 * (1 - grad_t) + 14 * grad_t
    bg_g = 30 * (1 - grad_t) + 16 * grad_t
    bg_b = 39 * (1 - grad_t) + 20 * grad_t

    if is_border:
        return (66, 133, 244, int(255 * edge_alpha * 0.85))

    col_r, col_g, col_b, _ = render_content(nx, ny, bg_r, bg_g, bg_b)
    return (col_r, col_g, col_b, int(255 * edge_alpha))

def render_vrc_round_icon(x, y, w, h):
    nx = (x / (w - 1)) * 2.0 - 1.0
    ny = (y / (h - 1)) * 2.0 - 1.0

    r_dist = math.hypot(nx, ny)

    if r_dist > 1.02:
        return (0, 0, 0, 0)

    edge_alpha = 1.0
    if r_dist > 0.98:
        edge_alpha = (1.02 - r_dist) / 0.04

    is_border = (r_dist >= 0.94)

    grad_t = 0.5 * (nx * 0.3 - ny * 0.7 + 1.0)
    bg_r = 26 * (1 - grad_t) + 14 * grad_t
    bg_g = 30 * (1 - grad_t) + 16 * grad_t
    bg_b = 39 * (1 - grad_t) + 20 * grad_t

    if is_border:
        return (66, 133, 244, int(255 * edge_alpha * 0.85))

    col_r, col_g, col_b, _ = render_content(nx, ny, bg_r, bg_g, bg_b)
    return (col_r, col_g, col_b, int(255 * edge_alpha))

def render_vrc_fg(x, y, w, h):
    nx = (x / (w - 1)) * 2.0 - 1.0
    ny = (y / (h - 1)) * 2.0 - 1.0

    scale = 0.72
    snx = nx / scale
    sny = ny / scale

    if abs(snx) > 1.0 or abs(sny) > 1.0:
        return (0, 0, 0, 0)

    col_r, col_g, col_b, has_pixel = render_content(snx, sny, 0, 0, 0)
    if has_pixel:
        return (col_r, col_g, col_b, 255)
    else:
        return (0, 0, 0, 0)

def main():
    print("Rendering official VRC Py-Script icons...")

    # 1. Favicons
    create_png("favicon.png", 128, 128, render_vrc_icon)
    create_png("favicon-32x32.png", 32, 32, render_vrc_icon)
    create_png("favicon-16x16.png", 16, 16, render_vrc_icon)

    # 2. Main Logos
    create_png("logo.png", 512, 512, render_vrc_icon)
    create_png("vrc_launcher.png", 512, 512, render_vrc_icon)

    # 3. Drawables
    create_png("Vrc/src/main/res/drawable/ic_launcher_fg_rendered.png", 432, 432, render_vrc_fg)
    create_png("Vrc/src/main/res/drawable/img_vrc_logo.png", 256, 256, render_vrc_icon)
    create_png("Vrc/src/main/res/drawable/img_vrc_emblem.png", 128, 128, render_vrc_icon)
    create_png("Vrc/src/main/assets/logo.png", 512, 512, render_vrc_icon)
    create_png("Vrc/src/main/assets/favicon.png", 128, 128, render_vrc_icon)

    # 4. Mipmaps
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    for folder, sz in sizes.items():
        create_png(f"Vrc/src/main/res/{folder}/ic_launcher.png", sz, sz, render_vrc_icon)
        create_png(f"Vrc/src/main/res/{folder}/ic_launcher_round.png", sz, sz, render_vrc_round_icon)

    print("All official VRC icons created and deployed successfully!")

if __name__ == "__main__":
    main()
