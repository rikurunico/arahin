import { P2pClient } from '../wearengine.js'
import vibrator from '@system.vibrator'
import battery from '@system.battery'

const PHONE_PKG = 'com.arahin'
const PHONE_FINGERPRINT = '28DAEBEF9CCBF3125D209F4477F65BD2FEB45065667379C50801F415DFD037D9'

const IMG = '/common/images/'
// d: S straight, L R turn, l r slight, h j sharp, U u-turn, O roundabout, D destination, M merge, E exit, Q leaving a roundabout, ? plain text
const ICON = {
  S: 'arrow-s', M: 'arrow-s', E: 'arrow-s', Q: 'arrow-s', '?': 'arrow-s',
  L: 'arrow-l', R: 'arrow-r', l: 'arrow-sl', r: 'arrow-sr', h: 'arrow-hl', j: 'arrow-hr',
  U: 'arrow-u', O: 'arrow-o', D: 'arrow-d',
}
const DRIVE_LEFT = true // Indonesia: roundabouts run clockwise, exit 1 is on the left; set false for right-hand traffic
const LEFT = 'Llh'
const RIGHT = 'Rrj'
const TRACK = 140 // px, keep equal to .track width in index.css
const NEAR = 300 // metres where the bar starts filling
const ALERTS = [500, 200, 50] // metres where the watch buzzes; a turn first seen farther away stays silent
// the distance turns yellow within 200 m and orange within 50 m, so a glance tells how close the turn is
const COLD = '#ffffff'
const WARM = '#ffc700'
const HOT = '#ff6a3d'
const LONG_INSTR = 16 // characters; longer instructions get a smaller font so they stay on screen
const VERSION = 'v1.0.5' // keep equal to app.version.name in the module's config.json (code 1000005)

export default {
  data: {
    instr: 'Menunggu rute',
    instrSize: 26,
    arrow: IMG + 'arrow-s.png',
    dist: '',
    distColor: COLD,
    eta: '',
    fill: 0,
    clock: '',
    battery: '', // phone battery, sent along with every turn
    watchBattery: '',
    version: VERSION, // always on screen for now, so a photo of the watch tells which install it is
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
    // the watch battery moves slowly, so ask for it once a minute
    this.readBattery()
    this.batteryTimer = setInterval(() => page.readBattery(), 60000)
  },

  readBattery() {
    const page = this
    try {
      battery.getStatus({
        success(data) {
          page.watchBattery = 'Jam ' + Math.round(data.level * 100) + '%'
        },
        fail() {
          page.watchBattery = ''
        },
      })
    } catch (e) {
      page.watchBattery = ''
    }
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
    const size = m.t.length > LONG_INSTR ? 22 : 26
    if (this.instrSize !== size) this.instrSize = size
    if (this.arrow !== arrow) this.arrow = arrow
    if (this.dist !== m.m) this.dist = m.m
    const color = m.n < 0 || m.n > ALERTS[1] ? COLD : m.n > ALERTS[2] ? WARM : HOT
    if (this.distColor !== color) this.distColor = color
    if (this.eta !== m.e) this.eta = m.e
    // the clock comes from the phone: the watch's own Date is not trusted on this engine
    if (m.c && this.clock !== m.c) this.clock = m.c
    const battery = m.b >= 0 ? 'HP ' + m.b + '%' : ''
    if (this.battery !== battery) this.battery = battery
    const fill = m.n < 0 ? 0 : Math.round(TRACK * Math.max(0, Math.min(1, 1 - m.n / NEAR)))
    if (this.fill !== fill) this.fill = fill
  },

  // left = 1 short, right = 2 short, u-turn = 1 long, roundabout = long + short, destination = 2 long;
  // straight, merge, exits and plain text stay silent
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
    clearInterval(this.batteryTimer)
    this.p2pClient.unregisterReceiver({ onSuccess() {}, onFailure() {} })
  },
}
