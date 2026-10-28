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

export default {
  data: {
    instr: 'Menunggu rute',
    arrow: DIRS.S.arrow,
    dist: '',
    eta: '',
  },

  onInit() {
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
    if (d.arrow !== this.arrow) vibrator.vibrate({ mode: 'short' })
    this.instr = d.instr
    this.arrow = d.arrow
    this.dist = m.m || ''
    this.eta = m.e || ''
  },

  onDestroy() {
    this.p2pClient.unregisterReceiver({ onSuccess() {}, onFailure() {} })
  },
}
