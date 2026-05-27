// header.js
document.addEventListener("DOMContentLoaded", function() {
    // 1. 헤더용 CSS 캡슐화 주입
    const style = document.createElement('style');
    style.innerHTML = `
        .global-header {
            display: flex; justify-content: space-between; align-items: center;
            padding: 0 24px; height: 64px; background: #ffffff;
            border-bottom: 1px solid #e2e8f0; position: sticky; top: 0; z-index: 1000;
            font-family: 'Pretendard', sans-serif; flex-shrink: 0;
        }
        .global-header .logo { font-size: 1.2rem; font-weight: 800; color: #0f172a; text-decoration: none; }
        .global-header .nav-links { display: flex; gap: 8px; }
        .global-header .nav-links a {
            padding: 8px 16px; border-radius: 8px; font-size: 0.95rem; font-weight: 600;
            color: #64748b; text-decoration: none; transition: 0.2s;
        }
        .global-header .nav-links a:hover { background: #f8fafc; color: #0f172a; }
        .global-header .nav-links a.active { background: #eff6ff; color: #3b82f6; }
        
        .global-header .header-right { display: flex; align-items: center; gap: 16px; }
        .global-header .user-menu { position: relative; cursor: pointer; display: flex; align-items: center; gap: 8px; padding: 6px 12px 6px 6px; border-radius: 30px; border: 1px solid #e2e8f0; }
        .global-header .user-menu:hover { background: #f8fafc; }
        .global-header .avatar { width: 28px; height: 28px; background: #3b82f6; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: bold; }
        .global-header .username { font-size: 0.85rem; font-weight: 600; color: #0f172a; }
        
        .global-header .dropdown {
            display: none; position: absolute; top: 45px; right: 0; width: 160px;
            background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); overflow: hidden;
        }
        .global-header .dropdown.show { display: block; }
        .global-header .dropdown button {
            width: 100%; padding: 12px 16px; text-align: left; border: none; background: none;
            font-size: 0.9rem; font-weight: 600; color: #0f172a; cursor: pointer;
        }
        .global-header .dropdown button:hover { background: #f8fafc; }
        .global-header .dropdown button.danger { color: #ef4444; }
        
        .global-header .settings-btn { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #64748b; padding: 4px; }
    `;
    document.head.appendChild(style);

    // 2. 헤더 HTML 요소 생성
    const currentPath = window.location.pathname;
    const headerHTML = `
        <header class="global-header">
            <a href="/index.html" class="logo">📘 나의 스터디 메이트</a>
            
            <nav class="nav-links">
                <a href="/index.html" class="${currentPath === '/' || currentPath.includes('index') ? 'active' : ''}">대화하기</a>
                <a href="/summary.html" class="${currentPath.includes('summary') ? 'active' : ''}">학습 요약표</a>
                <a href="/dashboard.html" class="${currentPath.includes('dashboard') ? 'active' : ''}">통계 대시보드</a>
            </nav>

            <div class="header-right">
                <div class="user-menu" id="headerUserMenu">
                    <div class="avatar">U</div>
                    <span class="username" id="headerUserName">메뉴 ▾</span>
                    
                    <div class="dropdown" id="headerDropdown">
                        <button onclick="window.location.href='/settings.html'">사용자 설정</button>
                        <button class="danger" onclick="executeLogout()">로그아웃</button>
                    </div>
                </div>
                <button class="settings-btn" id="globalSettingsBtn" title="음성 설정">⚙️</button>
            </div>
        </header>
    `;

    // 3. body의 맨 앞에 헤더 삽입
    document.body.insertAdjacentHTML('afterbegin', headerHTML);

    // 4. JWT 토큰 디코딩 로직
    const token = localStorage.getItem('jwt_token');
    if (token) {
        try {
            const payload = JSON.parse(decodeURIComponent(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')));
            document.getElementById('headerUserName').textContent = payload.sub.split('@')[0] + ' ▾';
        } catch(e) {}
    }

    // 5. 드롭다운 토글 및 바깥 클릭 시 닫기
    const userMenu = document.getElementById('headerUserMenu');
    const dropdown = document.getElementById('headerDropdown');
    userMenu.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('show');
    });
    window.addEventListener('click', (e) => {
        if (!userMenu.contains(e.target)) dropdown.classList.remove('show');
    });

    // 전역 로그아웃 함수
    window.executeLogout = function() {
        if(confirm('로그아웃 하시겠습니까?')) {
            localStorage.removeItem('jwt_token');
            window.location.href = '/login.html';
        }
    };
});