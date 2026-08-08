import { afterEach } from 'vitest'

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver = ResizeObserverStub
globalThis.matchMedia = globalThis.matchMedia || (() => ({
  matches: false,
  addEventListener() {},
  removeEventListener() {}
}))
globalThis.HTMLElement.prototype.scrollIntoView = () => {}

afterEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
})
