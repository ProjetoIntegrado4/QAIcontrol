const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;
const API = process.env.API_BASE_URL || 'http://localhost:8080';

app.use(express.json());

/*
 * BFF didático: preserva exatamente o path /api/* enviado pelo browser.
 * Ex.: /api/projects -> http://backend:8080/api/projects
 *      /api/usages   -> http://backend:8080/api/usages
 */
app.use('/api', async (req, res) => {
  const targetUrl = `${API}${req.originalUrl}`;

  try {
    console.log(`[BFF] ${req.method} ${req.originalUrl} -> ${targetUrl}`);

    const options = {
      method: req.method,
      headers: {
        'Accept': req.headers.accept || 'application/json'
      }
    };

    if (!['GET', 'HEAD'].includes(req.method)) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(req.body ?? {});
    }

    const response = await fetch(targetUrl, options);
    const contentType = response.headers.get('content-type');
    const body = await response.text();

    res.status(response.status);
    if (contentType) {
      res.set('Content-Type', contentType);
    }
    res.send(body);
  } catch (err) {
    console.error('[BFF] Backend indisponível:', err.message);
    res.status(502).json({
      error: 'Backend indisponível',
      detail: err.message
    });
  }
});

app.use(express.static('public'));

app.listen(PORT, () => {
  console.log(`Front em http://localhost:${PORT}`);
  console.log(`BFF /api/* -> ${API}/api/* (path preservado)`);
});
