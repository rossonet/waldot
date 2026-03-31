// Script mode test configuration
// This file demonstrates the new Groovy script format

// Function to create a temperature sensor
def createTempSensor(name, min, max) {
  g.addV('generator')
    .property('type', 'generator')
    .property('label', name)
    .property('Algorithm', 'sinusoidal')
    .property('Min', min.toString())
    .property('Max', max.toString())
    .property('Delay', '5000')
    .next()
}

// Create multiple sensors
office = createTempSensor('office-temp', 18, 26)
serverRoom = createTempSensor('server-temp', 16, 22)
warehouse = createTempSensor('warehouse-temp', 10, 30)

// Log configuration
log.info("Test configuration loaded: 3 sensors created")
