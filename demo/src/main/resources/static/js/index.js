document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');

    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;

        // 发送登录请求到服务器
        fetch(`http://8.130.130.240:8088/api/user/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        })
            .then(response => response.json())
            .then(data => {
                console.log('Response data:', data); // 打印收到的响应数据
                if (data.code === 20000) { // 假设返回的成功状态码为20000
                    // 登录成功，保存 token 和用户信息到 localStorage
                    const user = {
                        username: data.data.name,
                        token: data.data.token,
                        id: data.data.id
                    };
                    localStorage.setItem('user', JSON.stringify(user));
                    window.location.href = '/fileList';  // 重定向到/adminHome，由控制器处理
                } else {
                    // 处理登录失败情况
                    alert('Login failed: ' + data.data.error);
                }
            })
            .catch(error => {
                console.error('Error during login:', error);
                alert('Login failed. Please try again.');
            });
    });

    const container = document.querySelector('#container');
    const signInButton = document.querySelector('#signIn');
    const signUpButton = document.querySelector('#signUp');

    signUpButton.addEventListener('click', () => container.classList.add('right-panel-active'));
    signInButton.addEventListener('click', () => container.classList.remove('right-panel-active'));
});



