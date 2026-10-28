import { P2pClient } from '../wearengine.js'
import vibrator from '@system.vibrator'

const PHONE_PKG = 'com.arahin'
const PHONE_FINGERPRINT = 'REPLACE_WITH_PHONE_SIGNING_FINGERPRINT'

const IMG = '/common/images/'
// d: S straight, L R turn, l r slight, h j sharp, U u-turn, O roundabout, D destination, M merge, ? plain text
const ICON = {
  S: 'arrow-s', M: 'arrow-s', '?': 'arrow-s',
  L: 'arrow-l', R: 'arrow-r', l: 'arrow-sl', r: 'arrow-sr', h: 'arrow-hl', j: 'arrow-hr',
  U: 'arrow-u', O: 'arrow-o', D: 'arrow-d',
}
const DRIVE_LEFT = true // Indonesia: roundabouts run clockwise, exit 1 is on the left; set false for right-hand traffic
const LEFT = 'Llh'
const RIGHT = 'Rrj'
const TRACK = 280 // px, keep equal to .track width in index.css
const NEAR = 300 // metres where the bar starts filling
const ALERTS = [500, 200, 50] // metres where the watch buzzes; a turn first seen farther away stays silent

export default {
  data: {
    instr: 'Menunggu rute',
    arrow: IMG + 'arrow-s.png',
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
    let icon = ICON[m.d]
    if (!icon) return
    // schematic 4-arm roundabout: exits 1-3 get their own icon, anything else the generic one
    if (m.d === 'O' && m.x >= 1 && m.x <= 3) icon += DRIVE_LEFT ? m.x : 4 - m.x

    const turned = m.d !== this.dir && m.n >= 0 && m.n <= ALERTS[0]
    const crossed = ALERTS.some((a) => this.meters > a && m.n >= 0 && m.n <= a)
    if (turned || crossed) this.buzz(m.d)
    this.dir = m.d
    this.meters = m.n

    // only touch what changed, so the UI re-renders as little as possible
    const arrow = IMG + icon + '.png'
    if (this.instr !== m.t) this.instr = m.t
    if (this.arrow !== arrow) this.arrow = arrow
    if (this.dist !== m.m) this.dist = m.m
    if (this.eta !== m.e) this.eta = m.e
    const fill = m.n < 0 ? 0 : Math.round(TRACK * Math.max(0, Math.min(1, 1 - m.n / NEAR)))
    if (this.fill !== fill) this.fill = fill
  },

  // left = 1 short, right = 2 short, u-turn = 1 long, roundabout = long + short, destination = 2 long;
  // straight, merge and plain text stay silent
  buzz(dir) {
    const again = (mode, ms) => setTimeout(() => vibrator.vibrate({ mode }), ms)
    if (LEFT.indexOf(dir) >= 0) return vibrator.vibrate({ mode: 'short' })
    if (RIGHT.indexOf(dir) >= 0) {
      vibrator.vibrate({ mode: 'short' })
      return again('short', 300)
    }
    if (dir === 'U') return vibrator.vibrate({ mode: 'long' })
    if (dir === 'O') {
      vibrator.vibrate({ mode: 'long' })
      return again('short', 600)
    }
    if (dir === 'D') {
      vibrator.vibrate({ mode: 'long' })
      return again('long', 900)
    }
  },

  onDestroy() {
    this.p2pClient.unregisterReceiver({ onSuccess() {}, onFailure() {} })
  },
}
