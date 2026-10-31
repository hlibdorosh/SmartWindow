require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const plantsRouter = require('./routes/plants');

const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI;

if (!MONGODB_URI) {
  console.error('MONGODB_URI is not set. Copy .env.example to .env and fill it in.');
  process.exit(1);
}

const app = express();
app.use(express.json());

app.get('/health', (req, res) => res.json({ ok: true }));
app.use('/plants', plantsRouter);

// Mongoose validation errors -> 400, everything else -> 500
app.use((err, req, res, next) => {
  if (err.name === 'ValidationError' || err.name === 'CastError' || err.code === 11000) {
    const status = err.code === 11000 ? 409 : 400;
    return res.status(status).json({ error: err.message });
  }
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});

mongoose
  .connect(MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB');
    app.listen(PORT, () => console.log(`plant-api listening on port ${PORT}`));
  })
  .catch((err) => {
    console.error('MongoDB connection failed:', err.message);
    process.exit(1);
  });
