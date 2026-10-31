require('dotenv').config();
const mongoose = require('mongoose');
const Plant = require('../src/models/Plant');
const plants = require('./plants.json');

async function main() {
  if (!process.env.MONGODB_URI) {
    throw new Error('MONGODB_URI is not set');
  }
  await mongoose.connect(process.env.MONGODB_URI);

  // Replace the whole catalog so the seed is repeatable
  await Plant.deleteMany({});
  const inserted = await Plant.insertMany(plants);
  console.log(`Seeded ${inserted.length} plants`);
}

main()
  .catch((err) => {
    console.error('Seed failed:', err.message);
    process.exitCode = 1;
  })
  .finally(() => mongoose.disconnect());
