# Data Generation Algorithms

## Algorithm Comparison

| Algorithm   | Pattern      | Period    | Smoothness | Use Cases                          |
|-------------|--------------|-----------|------------|------------------------------------|
| incremental | Linear up    | Configurable | Steps     | Counters, IDs, timers             |
| decremental | Linear down  | Configurable | Steps     | Countdown, reverse counters       |
| random      | Noise        | None      | Discrete  | Sensors with noise, unpredictable |
| sinusoidal  | Sine wave    | 2π ≈ 6.28 | Smooth    | Temperature cycles, AC voltage    |
| triangular  | Triangle     | π ≈ 3.14  | Linear    | Sawtooth, ramps                   |
| stopped     | Constant     | Infinite  | Flat      | Reference values                  |

## Detailed Algorithms

### Incremental

**Formula:** `value++; if (value > max) value = min`

**Characteristics:**
- Linear increase by 1 per update
- Wrap-around at max boundary
- Deterministic and predictable

**Graph:**
```
Value
  │
Max ────────┐
  │         │
  │         │
  │        ││
Min ────────┘
  └─────────────> Time
```

**Example:**
- Min=0, Max=10: `0,1,2,3,4,5,6,7,8,9,10,0,1,2,...`

**Use Cases:**
- Sequential IDs
- Production counters
- Simple timers

---

### Decremental

**Formula:** `value--; if (value < min) value = max`

**Characteristics:**
- Linear decrease by 1 per update
- Wrap-around at min boundary
- Deterministic and predictable

**Graph:**
```
Value
  │
Max ────────┘
  │         │
  │         │
  │        ││
Min ────────┐
  └─────────────> Time
```

**Example:**
- Min=0, Max=10: `10,9,8,7,6,5,4,3,2,1,0,10,9,8,...`

**Use Cases:**
- Countdown timers
- Reverse counters
- Depletion simulation

---

### Random

**Formula:** `value = Math.random() * (max - min) + min`

**Characteristics:**
- Uniform distribution in [min, max]
- No correlation between consecutive values
- Non-deterministic

**Graph:**
```
Value
  │  ●   ● ●  
Max ─────●─●───
  │ ●  ●   ● ●
  │●  ●● ●   
Min ─●───●──●──
  └─────────────> Time
```

**Statistical Properties:**
- Mean: (max + min) / 2
- Standard deviation: (max - min) / √12

**Use Cases:**
- Noisy sensors
- Random events
- Simulation uncertainty

---

### Sinusoidal

**Formula:** `value = (max - min) / 2 * sin(seed++) + (max + min) / 2`

**Characteristics:**
- Smooth continuous curve
- Periodic with period 2π ≈ 6.28 updates
- Centered at midpoint (max+min)/2
- Amplitude (max-min)/2

**Graph:**
```
Value
  │    ╭─╮    ╭─╮
Max ───╯  ╰───╯  ╰──
  │              
Min ───╮  ╭───╮  ╭──
  │    ╰─╯    ╰─╯
  └─────────────────> Time
    2π    4π    6π
```

**Mathematical Properties:**
- Frequency: 1 / (2π * delay) Hz
- Period: 2π * delay milliseconds

**Use Cases:**
- Temperature cycles (day/night)
- AC voltage/current
- Tidal patterns
- Heartbeat simulation

---

### Triangular

**Formula:** `value = min + (max - min) * (2 / π * acos(|cos(seed++)|))`

**Characteristics:**
- Linear ramps up and down
- Period π ≈ 3.14 updates
- Sharp peaks at max
- Linear segments (not curved)

**Graph:**
```
Value
  │    ╱╲    ╱╲
Max ───╱  ╲──╱  ╲──
  │  ╱    ╲╱    ╲
Min ─╱          ╲╱
  └─────────────────> Time
    π     2π    3π
```

**Mathematical Properties:**
- Slope: ±(max-min) / (π/2) per update
- Period: π * delay milliseconds

**Use Cases:**
- Sawtooth patterns
- Linear ramps
- PWM simulation

---

### Stopped

**Formula:** `value = actualValue` (no change)

**Characteristics:**
- Value remains constant
- No CPU cycles for generation
- Useful for pausing without removal

**Graph:**
```
Value
  │
Max ──────────────
  │ ══════════════
  │
Min ──────────────
  └─────────────> Time
```

**Use Cases:**
- Reference voltage
- Paused simulation
- Static baselines

---

## Algorithm Selection Guide

### Matching Real-World Phenomena

| Phenomenon | Best Algorithm | Configuration Example |
|------------|----------------|----------------------|
| Room temperature (daily cycle) | sinusoidal | Min=18, Max=26, Delay=3600000 (1 hour) |
| Production counter | incremental | Min=0, Max=999999, Delay=60000 (1 min) |
| Sensor noise | random | Min=±5 around baseline |
| Sawtooth PWM | triangular | Min=0, Max=100, Delay=100 |
| Countdown timer | decremental | Max=3600, Min=0, Delay=1000 |

### Performance Considerations

**Fastest:**
1. stopped (no computation)
2. incremental/decremental (~10ns)
3. random (~20ns)

**Medium:**
4. sinusoidal (~50ns with Math.sin)

**Slowest:**
5. triangular (~100ns with multiple transcendental functions)

Note: All algorithms are fast enough for typical use (< 0.1µs per update).

---

## Custom Algorithm Implementation

To add custom algorithms, extend DataGeneratorVertex:

```java
public enum Algorithm {
    ..., custom_exponential
}

protected void generateNextExponential() {
    // Exponential decay
    actualValue = max * Math.exp(-seed++ * 0.1) + min;
    if (actualValue < min) {
        seed = 0;  // Reset
        actualValue = max;
    }
    assignValue();
}

// Add to switch in runner
case custom_exponential:
    generateNextExponential();
    break;
```

---

*Algorithms Guide for WaldOT Data Generator Plugin version 0.4.0+*
