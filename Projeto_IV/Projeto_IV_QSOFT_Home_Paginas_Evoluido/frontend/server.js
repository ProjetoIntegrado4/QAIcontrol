const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;
const API = process.env.API_BASE_URL || 'http://localhost:8080';

app.use(express.json());

app.use(async (req, res, next) => {
    if ((!req.path.endsWith('.html') && req.path !== '/') || req.path === '/login.html') return next();

    try {
        const response = await fetch(`${API}/api/auth/me`, {
            headers: {Cookie: req.headers.cookie || ''}
        });
        if (!response.ok) return res.redirect('/login.html');
        next();
    } catch (_err) {
        res.redirect('/login.html');
    }
});

// BFF: endpoint orientado à tela. Agrega dados de empresas e consumos.
app.get('/bff/dashboard', async (req, res) => {
    try {
        const headers = {Cookie: req.headers.cookie || ''};
        const [empresasResponse, usagesResponse] = await Promise.all([
            fetch(`${API}/api/empresa`, {headers}),
            fetch(`${API}/api/usages`, {headers})
        ]);

        if (!empresasResponse.ok || !usagesResponse.ok) {
            return res.status(502).json({error: 'Falha ao consultar o backend Java'});
        }

        const empresas = await empresasResponse.json();
        const usages = await usagesResponse.json();

        const totalTokens = usages.reduce((sum, usage) => sum + Number(usage.tokens || 0), 0);

        res.json({
            totalEmpresas: empresas.length,
            totalConsumos: usages.length,
            totalTokens,
            empresas,
            usages
        });
    } catch (err) {
        res.status(502).json({error: 'Backend indisponível', detail: err.message});
    }
});

// Proxy do BFF para operações CRUD/REST simples do front.
app.use('/api', async (req, res) => {
    const targetUrl = `${API}${req.originalUrl}`;

    try {
        console.log(`[BFF] ${req.method} ${req.originalUrl} -> ${targetUrl}`);

        const options = {
            method: req.method,
            headers: {'Accept': req.headers.accept || 'application/json'}
        };

        if (req.headers.cookie) options.headers.Cookie = req.headers.cookie;
        const csrfCookie = req.headers.cookie?.split('; ')
            .find((cookie) => cookie.startsWith('XSRF-TOKEN='))
            ?.substring('XSRF-TOKEN='.length);
        const csrfToken = req.headers['x-xsrf-token'] || (csrfCookie ? decodeURIComponent(csrfCookie) : '');
        if (csrfToken) options.headers['X-XSRF-TOKEN'] = csrfToken;

        if (!['GET', 'HEAD'].includes(req.method)) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(req.body ?? {});
        }

        const response = await fetch(targetUrl, options);
        const contentType = response.headers.get('content-type');
        const body = await response.text();
        const setCookies = response.headers.getSetCookie?.() || [];

        res.status(response.status);
        if (contentType) res.set('Content-Type', contentType);
        if (setCookies.length) res.append('Set-Cookie', setCookies);
        res.send(body);
    } catch (err) {
        res.status(502).json({error: 'Backend indisponível', detail: err.message});
    }
});

app.use(express.static('public'));

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Front/BFF em http://localhost:${PORT}`);
    console.log(`Java backend: ${API}`);
});
