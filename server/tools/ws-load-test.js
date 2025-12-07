import WebSocket from 'ws'

const clientCount = parseInt(process.argv[2] || '5', 10)
const messagesPerClient = parseInt(process.argv[3] || '20', 10)
const url = process.env.WS_URL || 'ws://localhost:4455/ws'
const token = process.env.DECK_TOKEN || 'change-me'

const latencies = []
let activeClients = 0

function mean(values) {
  return values.reduce((sum, v) => sum + v, 0) / (values.length || 1)
}

function percentile(values, p) {
  if (!values.length) return 0
  const sorted = [...values].sort((a, b) => a - b)
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[idx]
}

function printSummary() {
  console.log('\nLoad test completed:')
  console.log(`  Clients: ${clientCount}`)
  console.log(`  Messages/client: ${messagesPerClient}`)
  console.log(`  Samples: ${latencies.length}`)
  console.log(`  Mean latency: ${mean(latencies).toFixed(2)}ms`)
  console.log(`  P50: ${percentile(latencies, 50).toFixed(2)}ms`)
  console.log(`  P95: ${percentile(latencies, 95).toFixed(2)}ms`)
  console.log(`  P99: ${percentile(latencies, 99).toFixed(2)}ms`)
}

function spawnClient(index) {
  const ws = new WebSocket(url, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  activeClients += 1
  const pending = new Map()

  ws.on('open', () => {
    for (let i = 0; i < messagesPerClient; i += 1) {
      const messageId = `${index}-${i}-${Date.now()}`
      pending.set(messageId, Date.now())
      ws.send(
        JSON.stringify({
          kind: 'control',
          controlId: `load_test_${index}`,
          type: 'BUTTON',
          value: 1,
          messageId,
          sentAt: Date.now(),
        })
      )
    }
  })

  ws.on('message', (raw) => {
    try {
      const payload = JSON.parse(raw.toString())
      if (payload.type === 'ack' && payload.messageId) {
        const started = pending.get(payload.messageId)
        if (started) {
          latencies.push(Date.now() - started)
          pending.delete(payload.messageId)
        }
        if (pending.size === 0) {
          ws.close(1000, 'completed')
        }
      }
    } catch (error) {
      console.error('Unable to parse message from server', error)
    }
  })

  ws.on('close', () => {
    activeClients -= 1
    if (activeClients === 0) {
      printSummary()
    }
  })

  ws.on('error', (err) => {
    console.error('Client error', err.message)
  })
}

for (let i = 0; i < clientCount; i += 1) {
  spawnClient(i)
}
