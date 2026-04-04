# Example 4: Quality Control & Traceability

**Track product quality and genealogy through production process**

## Overview

Complete quality control system with batch tracking, defect detection, and product genealogy.

### Features

- **Batch Tracking**: Track raw materials through production
- **Quality Checkpoints**: Automated quality checks at each stage
- **Defect Detection**: Real-time defect identification
- **Product Genealogy**: Trace products back to raw materials

## Quick Start

```bash
cd /work/waldot/docs/examples/04-quality-control
docker-compose up -d
```

**Access**: `opc.tcp://localhost:12689/waldot`

## Architecture

```
Raw Material Batch
       ↓
   Processing
       ↓
  Quality Check → [PASS/FAIL]
       ↓
   Assembly
       ↓
  Final Inspection
       ↓
   Finished Product
```

## Sensors

- **quality-checkpoint-1**: Incoming material quality (95-100%)
- **quality-checkpoint-2**: Processing quality (90-100%)
- **quality-checkpoint-3**: Assembly quality (92-100%)
- **quality-checkpoint-4**: Final inspection (95-100%)
- **defect-detector**: Defect rate (0-5%)

## Rules

- **quality-gate**: Block batch if quality < 95%
- **defect-alert**: Alert if defects > 2%
- **batch-tracking**: Record batch genealogy
- **quarantine**: Quarantine failed batches

## Gremlin Queries

```groovy
// Find all batches
g.V().has('type','batch').valueMap('label','status')

// Trace product genealogy
g.V().has('label','BATCH-001').repeat(out('derived-from')).emit().path().by('label')

// Find failed batches
g.V().has('type','batch').has('status','FAILED').values('label')

// Quality statistics
g.V().has('type','generator').has('label',containing('quality')).values('data').mean()
```

---

**See**: [docker-compose.yml](docker-compose.yml) | [boot.conf](boot.conf)
