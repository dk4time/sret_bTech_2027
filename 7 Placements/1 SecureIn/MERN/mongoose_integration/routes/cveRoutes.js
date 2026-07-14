import express from "express";
import axios from "axios";
import CVE from "../models/CVE.js";

const router = express.Router();

/*
====================================================
FETCH AND STORE MULTIPLE CVEs
====================================================
*/
router.get("/sync", async (req, res) => {
  try {
    /*
    ====================================================
    FETCH API
    ====================================================
    */

    const response = await axios.get(
      "https://services.nvd.nist.gov/rest/json/cves/2.0",
    );

    const vulnerabilities = response.data.vulnerabilities;

    /*
    ====================================================
    COUNTER
    ====================================================
    */

    let processed = 0;

    /*
    ====================================================
    LOOP ALL CVEs
    ====================================================
    */

    for (const item of vulnerabilities) {
      const cve = item.cve;

      /*
      ==================================================
      BASIC FIELDS
      ==================================================
      */

      const cveId = cve.id;

      const sourceIdentifier = cve.sourceIdentifier;

      const published = cve.published;

      const lastModified = cve.lastModified;

      const vulnStatus = cve.vulnStatus;

      /*
      ==================================================
      DESCRIPTION
      ==================================================
      */

      const description =
        cve.descriptions?.find((d) => d.lang === "en")?.value || "";

      /*
      ==================================================
      METRICS
      ==================================================
      */

      const metric = cve.metrics?.cvssMetricV2?.[0];

      const score = metric?.cvssData?.baseScore;

      const severity = metric?.baseSeverity;

      const attackVector = metric?.cvssData?.accessVector;

      const attackComplexity = metric?.cvssData?.accessComplexity;

      const exploitabilityScore = metric?.exploitabilityScore;

      const impactScore = metric?.impactScore;

      /*
      ==================================================
      WEAKNESSES
      ==================================================
      */

      const weaknesses =
        cve.weaknesses?.flatMap((weakness) =>
          weakness.description.map((d) => d.value),
        ) || [];

      /*
      ==================================================
      REFERENCES
      ==================================================
      */

      const references = cve.references?.map((ref) => ref.url) || [];

      /*
      ==================================================
      CPES
      ==================================================
      */

      const cpes = [];

      cve.configurations?.forEach((config) => {
        config.nodes?.forEach((node) => {
          node.cpeMatch?.forEach((match) => {
            cpes.push(match.criteria);
          });
        });
      });

      /*
      ==================================================
      TRANSFORMED OBJECT
      ==================================================
      */

      const transformed = {
        cveId,
        sourceIdentifier,
        published,
        lastModified,
        vulnStatus,

        description,

        severity,
        score,

        attackVector,
        attackComplexity,

        exploitabilityScore,
        impactScore,

        weaknesses,
        references,
        cpes,

        raw: cve,
      };

      /*
      ==================================================
      UPSERT
      ==================================================
      */

      await CVE.updateOne(
        {
          cveId: transformed.cveId,
        },
        {
          $set: transformed,
        },
        {
          upsert: true,
        },
      );

      processed++;
    }

    /*
    ====================================================
    RESPONSE
    ====================================================
    */

    res.json({
      totalFetched: vulnerabilities.length,
      processed,
    });
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

export default router;
