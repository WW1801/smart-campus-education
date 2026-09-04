function showToast(message){const old=document.querySelector('.toast');if(old)old.remove();const toast=document.createElement('div');toast.className='toast';toast.textContent=message;document.body.append(toast);setTimeout(()=>toast.remove(),1800)}
function openTask(button,message){button.textContent='已打开';button.disabled=true;showToast(message);setTimeout(()=>{button.textContent='处理';button.disabled=false},1400)}
function toggleNav(){document.body.classList.toggle('nav-open')}
