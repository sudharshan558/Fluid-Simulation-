# Fluid-Simulation-
Real-time interactive fluid simulation built with JavaScript &amp; Canvas API — physics-based fluid dynamics in the browser.

# 🌊 Fluid Simulation

> Chaos that follows rules. Motion that feels alive.

## Live Preview

🌐 [View Demo](https://sudharshan558.github.io/fluid-simulation)

---

## What is this?

A real-time **interactive fluid simulation** running entirely in the browser using the **Canvas API** and **Vanilla JavaScript**. Move your mouse across the canvas and watch fluid dynamics come alive — velocity fields, pressure, diffusion, and advection all computed in real time.

No libraries. No WebGL frameworks. Pure math and code.

---

## How it works

The simulation is based on **Jos Stam's "Stable Fluids"** algorithm — a widely used method for real-time fluid simulation in games and visual effects.

### Core steps each frame:
```
1. Add velocity & density from user input (mouse movement)
2. Diffuse  → spread velocity & density across neighbouring cells
3. Advect   → move velocity & density along the flow field
4. Project  → enforce incompressibility (divergence-free field)
5. Render   → draw density field to canvas
```

---

## Features

| Feature | Detail |
|---------|--------|
| 🖱️ Mouse Interaction | Drag to inject fluid and velocity |
| 🎨 Colour Modes | Cycle through colour palettes |
| ⚡ Real-time | 60fps simulation on canvas |
| 🔢 Grid Based | Eulerian fluid solver on a 2D grid |
| 📱 Responsive | Adapts to window size |
| 🚀 Zero Dependencies | Pure HTML · CSS · Vanilla JS |

---

## Physics Concepts Used

- **Velocity field** — each grid cell stores a 2D velocity vector
- **Density field** — tracks how much "fluid" is in each cell
- **Diffusion** — fluid spreads to neighbouring cells over time
- **Advection** — fluid moves along its own velocity field
- **Projection** — ensures fluid is incompressible (mass conserved)
- **Linear solver** — Gauss-Seidel iterative method

---

## Tech Stack
```
HTML5       → Canvas element
CSS3        → Fullscreen layout
Vanilla JS  → Simulation loop, solver, rendering
Canvas API  → Pixel-level rendering
```

---

## Run Locally
```bash
git clone https://github.com/sudharshan558/fluid-simulation.git
cd fluid-simulation
open index.html
```

No build steps. No installs. Just open and run.

---

## Controls

| Input | Action |
|-------|--------|
| Mouse drag | Inject fluid + velocity |
| Mouse speed | Controls velocity magnitude |
| `C` key | Clear the canvas |
| `Space` | Pause / Resume |

---

## Screenshots

> *(Add screenshots or a GIF of the simulation here)*

---

## What I learned

- How real fluid solvers work under the hood
- Implementing iterative linear solvers (Gauss-Seidel)
- Working with 2D grid-based physics
- Optimising Canvas API rendering for 60fps
- Translating mathematical equations directly into code

---

## References

- Jos Stam — *"Real-Time Fluid Dynamics for Games"* (2003)
- Mike Ash — *"Fluid Simulation for Dummies"*

---

## Author

**Sudharshan R**
Programmer Analyst Trainee @ Cognizant · 2025
B.E. CSE — PSNA College of Engineering & Technology

[![GitHub](https://img.shields.io/badge/GitHub-sudharshan558-black?style=flat&logo=github)](https://github.com/sudharshan558)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Sudharshan%20R-blue?style=flat&logo=linkedin)](https://www.linkedin.com/in/sudharshan-ramalingam-257b60240/)

---

*Built with curiosity, math, and a lot of mouse dragging.*
