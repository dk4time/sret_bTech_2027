import mongoose from "mongoose";

const cveSchema = new mongoose.Schema(
  {
    /*
    ====================================================
    BASIC CVE INFORMATION
    ====================================================
    */

    cveId: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true,
    },

    sourceIdentifier: {
      type: String,
      trim: true,
    },

    vulnStatus: {
      type: String,
      index: true,
    },

    /*
    ====================================================
    IMPORTANT DATES
    ====================================================
    */

    published: {
      type: Date,
      index: true,
    },

    lastModified: {
      type: Date,
    },

    /*
    ====================================================
    DESCRIPTION
    ----------------------------------------------------
    We store only the English description as String
    instead of full multilingual descriptions array.
    ====================================================
    */

    description: {
      type: String,
      trim: true,
    },

    /*
    ====================================================
    CVSS / SECURITY METRICS
    ----------------------------------------------------
    Flattened from nested metrics object for easier
    querying and indexing.
    ====================================================
    */

    severity: {
      type: String,
      index: true,
    },

    score: {
      type: Number,
      index: true,
    },

    attackVector: {
      type: String,
    },

    attackComplexity: {
      type: String,
    },

    exploitabilityScore: {
      type: Number,
    },

    impactScore: {
      type: Number,
    },

    /*
    ====================================================
    ARRAYS
    ====================================================
    */

    weaknesses: [
      {
        type: String,
      },
    ],

    references: [
      {
        type: String,
      },
    ],

    cpes: [
      {
        type: String,
      },
    ],

    /*
    ====================================================
    RAW RESPONSE
    ----------------------------------------------------
    Stores original CVE object from API.
    Useful for:
    - debugging
    - future extraction
    - audit/reference
    ====================================================
    */

    raw: {
      type: mongoose.Schema.Types.Mixed,
    },
  },
  {
    timestamps: true,
  },
);

/*
====================================================
INDEXES
====================================================
*/

// Query optimization
cveSchema.index({
  severity: 1,
  score: -1,
});

// Sorting optimization
cveSchema.index({
  published: -1,
});

// Full text search
cveSchema.index({
  cveId: "text",
  description: "text",
});

const CVE = mongoose.model("CVE", cveSchema);

export default CVE;
