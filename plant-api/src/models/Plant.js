const mongoose = require('mongoose');

// Range of values for one sensor: min <= perfect <= max
const rangeValidator = {
  validator: (r) => r.min <= r.perfect && r.perfect <= r.max,
  message: 'range must satisfy min <= perfect <= max',
};

const rangeSchema = new mongoose.Schema(
  {
    min: { type: Number, required: true },
    perfect: { type: Number, required: true },
    max: { type: Number, required: true },
  },
  { _id: false }
);

const plantSchema = new mongoose.Schema(
  {
    plantName: { type: String, required: true, unique: true, trim: true },
    description: { type: String, default: '' },
    imageUrl: { type: String, default: null },

    temp: { type: rangeSchema, required: true, validate: rangeValidator }, // °C
    humidity: { type: rangeSchema, required: true, validate: rangeValidator }, // %
    light: { type: rangeSchema, required: true, validate: rangeValidator }, // %

    wateringIntervalDays: { type: Number, required: true, min: 1 },
    wateringNotes: { type: String, default: '' },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Plant', plantSchema, 'plants');
