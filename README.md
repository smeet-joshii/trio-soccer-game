# Trio Soccer Game

This was our final project for Software Design & Development at Kirkwood. Three of us (me, Tabir, and Eli) built a full 2D soccer game in Java from scratch — no game engine, no premade assets, just Java's built-in Graphics2D. Took us about 4 weeks working in sprints.

## What it does
It's a 3-vs-3 soccer game (1 goalie + 2 forwards per side). You can move your player around, dribble, charge up a kick, pass, and switch which player you're controlling mid-play. First to score more goals wins.

## Controls
- **WASD / Arrow Keys** — move
- **Hold Space** — charge up your kick
- **Shift** — max power shot
- **C** — pass to a teammate
- **X** — switch to another player

## Running it

javac *.java
java Starter


## How we built it
We planned everything out before writing code — UML diagrams for how the classes talk to each other, then split the work into `Field`, `Team`, `Player`, `Ball`, `GameTimer`, and `GameMenu` classes. Player class ended up being the biggest one — it handles movement, walk animation, kicking, and all the drawing (yeah, we hand-drew the little player sprites in code, legs and all).

We ran it Scrum-style — 4-week sprint, kanban board, the whole thing. The full product backlog (47 tasks) is in [`product-backlog.xlsx`](product-backlog.xlsx) if you want to see how we broke it down.

Honestly the hardest part was getting ball-player collision to feel right and not janky. Took a lot of trial and error on the friction values.

## Team
Smeet Joshi, Tabir, Eli
