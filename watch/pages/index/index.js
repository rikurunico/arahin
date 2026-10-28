import { P2pClient } from '../wearengine.js'
import vibrator from '@system.vibrator'

const PHONE_PKG = 'com.arahin'
const PHONE_FINGERPRINT = 'REPLACE_WITH_PHONE_SIGNING_FINGERPRINT'

const IMG = '/common/images/'
const DIRS = {
  S: { instr: 'Lurus', arrow: IMG + 'arrow-s.png' },
  L: { instr: 'Belok kiri', arrow: IMG + 'arrow-l.png' },
  R: { instr: 'Belok kanan', arrow: IMG + 'arrow-r.png' },
  U: { instr: 'Putar balik', arrow: IMG + 'arrow-u.png' },
}
const TRACK = 280 // px, keep equal to .track width in index.css
const NEAR = 300 // metres where the bar starts filling
const ALERTS = [500, 200, 50] // metres where the watch buzzes; a turn first seen farther away stays silent

export default {
  data: {
    instr: 'Menunggu rute',
    arrow: DIRS.S.arrow,
    dist: '',
    eta: '',
    fill: 0,
  },

  onInit() {
    this.dir = ''
    this.meters = -1
    this.p2pClient = new P2pClient()
    this.p2pClient.setPeerPkgName(PHONE_PKG)
    this.p2pClient.setPeerFingerPrint(PHONE_FINGERPRINT)
    const page = this
    this.p2pClient.registerReceiver({
      onSuccess() {},
      onFailure() {},
      onReceiveMessage(data) {
        if (data && !data.isFileType) page.show(data)
      },
    })
  },

  show(raw) {
    let m
    try {
      m = JSON.parse(raw)
    } catch (e) {
      return
    }
    const d = DIRS[m.d]
    if (!d) return

    const turned = m.d !== this.dir && m.n >= 0 && m.n <= ALERTS[0]
    const crossed = ALERTS.some((a) => this.meters > a && m.n >= 0 && m.n <= a)
    if (turned || crossed) this.buzz(m.d)
    this.dir = m.d
    this.meters = m.n

    // only touch what changed, so the UI re-renders as little as possible
    if (this.instr !== d.instr) this.instr = d.instr
    if (this.arrow !== d.arrow) this.arrow = d.arrow
    if (this.dist !== m.m) this.dist = m.m
    if (this.eta !== m.e) this.eta = m.e
    const fill = m.n < 0 ? 0 : Math.round(TRACK * Math.max(0, Math.min(1, 1 - m.n / NEAR)))
    if (this.fill !== fill) this.fill = fill
  },

  // left = 1 short, right = 2 short, u-turn = 1 long, straight = silent: readable without looking
  buzz(dir) {
    if (dir === 'U') return vibrator.vibrate({ mode: 'long' })
    if (dir !== 'L' && dir !== 'R') return
    vibrator.vibrate({ mode: 'short' })
    if (dir === 'R') setTimeout(() => vibrator.vibrate({ mode: 'short' }), 300)
  },

  onDestroy() {
    this.p2pClient.unregisterReceiver({ onSuccess() {}, onFailure() {} })
  },
}
