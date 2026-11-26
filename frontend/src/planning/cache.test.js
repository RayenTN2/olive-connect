import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCache, forget, invalidate, peek, refresh, retryStale, revalidate, staleIds, subscribe } from './cache.js'

const later = () => {
  let resolve
  let reject
  const promise = new Promise((ok, no) => { resolve = ok; reject = no })
  return { promise, resolve, reject }
}

describe('cache', () => {
  beforeEach(() => clearCache())

  it('sends the same list once when asked twice at the same moment', async () => {
    const load = vi.fn(async () => [1])
    await Promise.all([refresh('tours', load), refresh('tours', load)])
    expect(load).toHaveBeenCalledTimes(1)
    expect(peek('tours').data).toEqual([1])
  })

  it('keeps the old data during a refresh, and the same object when nothing changed', async () => {
    await refresh('tours', async () => [{ id: 1 }])
    const first = peek('tours').data
    const answer = later()
    const running = refresh('tours', () => answer.promise)
    expect(peek('tours').data).toBe(first)
    const redraw = vi.fn()
    const stop = subscribe(redraw)
    answer.resolve([{ id: 1 }])
    await running
    expect(peek('tours').data).toBe(first)
    expect(redraw).not.toHaveBeenCalled()
    await refresh('tours', async () => [{ id: 2 }])
    expect(peek('tours').data).toEqual([{ id: 2 }])
    expect(redraw).toHaveBeenCalledTimes(1)
    stop()
  })

  it('keeps the data when a refresh fails, says so, and recovers', async () => {
    await refresh('tours', async () => [1])
    await refresh('tours', async () => { throw new Error('down') })
    expect(peek('tours').data).toEqual([1])
    expect(staleIds()).toEqual(['tours'])
    let working = async () => [1]
    await refresh('tours', () => working())
    expect(staleIds()).toEqual([])
    working = async () => { throw new Error('down') }
    await refresh('tours', () => working())
    working = async () => [2]
    retryStale()
    await vi.waitFor(() => expect(peek('tours').data).toEqual([2]))
  })

  it('reports a first load that fails without calling it outdated', async () => {
    await refresh('tours', async () => { throw new Error('down') })
    expect(peek('tours').data).toBeUndefined()
    expect(peek('tours').error).toBeDefined()
    expect(staleIds()).toEqual([])
  })

  it('never shows an outdated list after a change, and loads it again', async () => {
    let value = ['old']
    await refresh('tours', async () => value)
    value = ['new']
    invalidate('tours')
    expect(peek('tours')).toBeUndefined()
    await vi.waitFor(() => expect(peek('tours')?.data).toEqual(['new']))
  })

  it('drops an answer asked before the change', async () => {
    const answers = [later(), later()]
    let call = 0
    const running = refresh('tours', () => answers[call++].promise)
    await Promise.resolve()
    invalidate('tours')
    answers[0].resolve(['old'])
    await running
    expect(peek('tours')).toBeUndefined()
    answers[1].resolve(['new'])
    await vi.waitFor(() => expect(peek('tours')?.data).toEqual(['new']))
  })

  it('keeps showing one item while it is read again', async () => {
    let value = 'draft'
    await refresh('tour|1', async () => value)
    value = 'confirmed'
    revalidate('tour|1')
    expect(peek('tour|1').data).toBe('draft')
    await vi.waitFor(() => expect(peek('tour|1').data).toBe('confirmed'))
  })

  it('forgets every period at once, even one still being asked', async () => {
    const answer = later()
    await refresh('free|a', async () => 1)
    await refresh('tours', async () => 2)
    const running = refresh('free|b', () => answer.promise)
    forget('free|')
    answer.resolve(3)
    await running
    expect([peek('free|a'), peek('free|b')]).toEqual([undefined, undefined])
    expect(peek('tours').data).toBe(2)
  })

  it('leaves nothing for the next account, even from an answer still on its way', async () => {
    await refresh('tours', async () => ['private'])
    const answer = later()
    const running = refresh('requests', () => answer.promise)
    clearCache()
    expect(peek('tours')).toBeUndefined()
    answer.resolve(['private'])
    await running
    expect(peek('requests')).toBeUndefined()
    await refresh('requests', async () => ['mine'])
    expect(peek('requests').data).toEqual(['mine'])
  })
})
