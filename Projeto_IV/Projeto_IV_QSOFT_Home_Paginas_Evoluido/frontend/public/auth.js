document.addEventListener('DOMContentLoaded', async () => {
    const navigation = document.querySelector('.top-nav');
    if (!navigation) return;

    try {
        const response = await fetch('/api/auth/me', {credentials: 'same-origin'});
        if (!response.ok) {
            window.location.replace('./login.html');
            return;
        }

        const user = await response.json();
        const logout = document.createElement('button');
        logout.type = 'button';
        logout.className = 'logout-button';
        logout.textContent = `Sair (${user.nome})`;
        logout.addEventListener('click', async () => {
            await fetch('/api/auth/csrf', {credentials: 'same-origin'});
            const csrfCookie = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='));
            const csrfToken = csrfCookie ? decodeURIComponent(csrfCookie.substring('XSRF-TOKEN='.length)) : '';
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'X-XSRF-TOKEN': csrfToken}
            });
            window.location.replace('./login.html');
        });
        navigation.appendChild(logout);
    } catch (_error) {
        window.location.replace('./login.html');
    }
});