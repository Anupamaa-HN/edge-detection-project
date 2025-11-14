const btn = document.getElementById('show') as HTMLButtonElement;
const input = document.getElementById('b64') as HTMLTextAreaElement;
const canvas = document.getElementById('canvas') as HTMLCanvasElement;
const fpsSpan = document.getElementById('fps') as HTMLSpanElement;
let last = performance.now();
btn.onclick = () => {
  const b64 = input.value.trim();
  if (!b64) return alert('Paste data:image/png;base64,...');
  const img = new Image();
  img.onload = () => {
    canvas.width = img.width; canvas.height = img.height;
    const ctx = canvas.getContext('2d')!;
    ctx.drawImage(img,0,0);
    const now = performance.now();
    const fps = Math.round(1000 / Math.max(1, (now - last)));
    fpsSpan.innerText = fps.toString();
    last = now;
  };
  img.src = b64;
};
