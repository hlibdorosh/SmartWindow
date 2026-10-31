const express = require('express');
const mongoose = require('mongoose');
const Plant = require('../models/Plant');

const router = express.Router();

// GET /plants?search=name
router.get('/', async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.search) {
      filter.plantName = { $regex: escapeRegex(req.query.search), $options: 'i' };
    }
    const plants = await Plant.find(filter).sort({ plantName: 1 });
    res.json(plants);
  } catch (err) {
    next(err);
  }
});

// GET /plants/:id
router.get('/:id', async (req, res, next) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ error: 'Invalid id' });
    }
    const plant = await Plant.findById(req.params.id);
    if (!plant) return res.status(404).json({ error: 'Plant not found' });
    res.json(plant);
  } catch (err) {
    next(err);
  }
});

// POST /plants
router.post('/', async (req, res, next) => {
  try {
    const plant = await Plant.create(req.body);
    res.status(201).json(plant);
  } catch (err) {
    next(err);
  }
});

// PUT /plants/:id
router.put('/:id', async (req, res, next) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ error: 'Invalid id' });
    }
    const plant = await Plant.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!plant) return res.status(404).json({ error: 'Plant not found' });
    res.json(plant);
  } catch (err) {
    next(err);
  }
});

// DELETE /plants/:id
router.delete('/:id', async (req, res, next) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ error: 'Invalid id' });
    }
    const plant = await Plant.findByIdAndDelete(req.params.id);
    if (!plant) return res.status(404).json({ error: 'Plant not found' });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});

function escapeRegex(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = router;
